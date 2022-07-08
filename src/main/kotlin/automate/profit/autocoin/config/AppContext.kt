package automate.profit.autocoin.config

import autocoin.metrics.JsonlFileStatsDClient
import automate.profit.autocoin.api.*
import automate.profit.autocoin.exchange.CachingPriceService
import automate.profit.autocoin.exchange.RestPriceService
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitOpportunitiesMonitorsProvider
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitCalculatorWithMetadata
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.CachingExchangeMetadataService
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.exchange.metadata.RestExchangeMetadataService
import automate.profit.autocoin.exchange.orderbook.OrderBookListeners
import automate.profit.autocoin.exchange.orderbookstream.OrderBookSseStreamService
import automate.profit.autocoin.exchange.ticker.TickerListeners
import automate.profit.autocoin.exchange.tickerstream.TickerSseStreamService
import automate.profit.autocoin.metrics.MetricsService
import automate.profit.autocoin.oauth.client.AccessTokenAuthenticator
import automate.profit.autocoin.oauth.client.AccessTokenInterceptor
import automate.profit.autocoin.oauth.client.ClientCredentialsAccessTokenProvider
import automate.profit.autocoin.oauth.server.AccessTokenChecker
import automate.profit.autocoin.oauth.server.Oauth2AuthenticationMechanism
import automate.profit.autocoin.oauth.server.Oauth2BearerTokenAuthHandlerWrapper
import automate.profit.autocoin.scheduled.HealthMetricsScheduler
import automate.profit.autocoin.scheduled.TwoLegOrderBookArbitrageProfitCacheScheduler
import com.timgroup.statsd.NonBlockingStatsDClient
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.sse.EventSources
import java.math.BigDecimal
import java.net.SocketAddress
import java.nio.file.Path
import java.time.Duration
import java.util.concurrent.Executors

class AppContext(private val appConfig: AppConfig) {
    companion object : KLogging()

    val httpClientWithoutAuthorization = OkHttpClient()
    val objectMapper = ObjectMapperProvider().createObjectMapper()
    val accessTokenProvider = ClientCredentialsAccessTokenProvider(
        httpClient = httpClientWithoutAuthorization,
        objectMapper = objectMapper,
        oauth2ServerUrl = appConfig.oauth2ServerUrl,
        oauthClientId = appConfig.arbitrageMonitorOauth2ClientId,
        oauthClientSecret = appConfig.arbitrageMonitorOauth2ClientSecret
    )
    val accessTokenAuthenticator = AccessTokenAuthenticator(accessTokenProvider)
    val accessTokenInterceptor = AccessTokenInterceptor(accessTokenProvider)
    val oauth2HttpClient = OkHttpClient.Builder()
        .authenticator(accessTokenAuthenticator)
        .addInterceptor(accessTokenInterceptor)
        .build()
    val sseHttpClient = oauth2HttpClient.newBuilder()
        .readTimeout(Duration.ofMillis(0L))
        .build()
    val sseEventSourceFactory = EventSources.createFactory(sseHttpClient)

    val statsdClient = if (appConfig.useMetrics) {
        NonBlockingStatsDClient(appConfig.serviceName, appConfig.telegrafHostname, 8125)
    } else {
        val metricsFolderPath = Path.of(appConfig.metricsFolder)
        metricsFolderPath.toFile().mkdirs()
        val metricsFile = metricsFolderPath.resolve("metrics.jsonl")
        logger.warn { "Using JsonlFileStatsDClient, telegraf.hostname not provided. Writing metrics to ${metricsFile.toAbsolutePath()}" }
        JsonlFileStatsDClient(metricsFile.toFile())
    }
    val metricsService: MetricsService = MetricsService(statsdClient)

    val priceService = CachingPriceService(
        decorated = RestPriceService(
            priceApiUrl = appConfig.exchangeMediatorApiUrl,
            httpClient = oauth2HttpClient,
            metricsService = metricsService,
            objectMapper = objectMapper
        )
    )

    val exchangeMetadataService = CachingExchangeMetadataService(
        decorated = RestExchangeMetadataService(
            httpClient = oauth2HttpClient,
            exchangeMetadataServiceHostWithPort = appConfig.exchangeMediatorApiUrl,
            objectMapper = objectMapper
        )
    )

    private val transactionFeeRatioWhenNotAvailableInMetadata = BigDecimal("0.001")

    val twoLegArbitrageProfitOpportunityCalculatorWithMetadata = TwoLegArbitrageProfitOpportunityCalculator(
        priceService = priceService,
        orderBookUsdAmountThresholds = appConfig.orderBookUsdAmountThresholds,
        relativeProfitCalculator = TwoLegArbitrageProfitCalculatorWithMetadata.DefaultBuilder(
            metadataService = exchangeMetadataService,
            transactionFeeRatioWhenNotAvailableInMetadata = transactionFeeRatioWhenNotAvailableInMetadata
        ).build(),
        metricsService = metricsService,
    )

    val twoLegArbitrageProfitOpportunityCache = TwoLegArbitrageProfitOpportunityCache(appConfig.ageOfOldestTwoLegArbitrageProfitToKeepInCacheMs)

    val scheduledJobsxecutorService = Executors.newScheduledThreadPool(3)

    private val twoLegOrderBookArbitrageProfitCacheScheduler = TwoLegOrderBookArbitrageProfitCacheScheduler(
        scheduledExecutorService = scheduledJobsxecutorService,
        ageOfOldestTwoLegArbitrageProfitToKeepMs = appConfig.ageOfOldestTwoLegArbitrageProfitToKeepInCacheMs,
        twoLegArbitrageProfitOpportunityCache = twoLegArbitrageProfitOpportunityCache,
        metricsService = metricsService
    )

    val orderBookListeners = OrderBookListeners()
    val tickerListeners = TickerListeners()
    val twoLegArbitrageMonitorProvider = TwoLegArbitrageProfitOpportunitiesMonitorsProvider(
        profitCache = twoLegArbitrageProfitOpportunityCache,
        profitCalculator = twoLegArbitrageProfitOpportunityCalculatorWithMetadata,
        metricsService = metricsService
    )

    val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(
        exchangeMetadataService = exchangeMetadataService,
        exchanges = appConfig.exchangesToMonitorTwoLegArbitrageOpportunities,
        currencyPairsWhiteList = appConfig.arbitrageCurrencyPairsWhiteList,
        staticTwoLegArbitrageCurrencyAndExchangePairs = appConfig.twoLegArbitrageCurrencyAndExchangePairs
    )

    val threadForStreamReconnecting = Executors.newSingleThreadExecutor()

    val orderBookSseStreamService = OrderBookSseStreamService(
        orderBookApiBaseUrl = appConfig.exchangeMediatorApiUrl,
        httpClient = sseHttpClient,
        eventSourceFactory = sseEventSourceFactory,
        orderBookListeners = orderBookListeners,
        objectMapper = objectMapper,
        executorForReconnecting = threadForStreamReconnecting
    )
    val tickerSseStreamService = TickerSseStreamService(
        tickerApiBaseUrl = appConfig.exchangeMediatorApiUrl,
        httpClient = sseHttpClient,
        eventSourceFactory = sseEventSourceFactory,
        tickerListeners = tickerListeners,
        objectMapper = objectMapper,
        executorForReconnecting = threadForStreamReconnecting
    )

    val healthMetricsScheduler = HealthMetricsScheduler(
        orderBookSseStreamService = orderBookSseStreamService,
        metricsService = metricsService,
        executorService = scheduledJobsxecutorService
    )

    val accessTokenChecker = AccessTokenChecker(httpClientWithoutAuthorization, objectMapper, appConfig)
    val oauth2AuthenticationMechanism = Oauth2AuthenticationMechanism(accessTokenChecker)
    val oauth2BearerTokenAuthHandlerWrapper = Oauth2BearerTokenAuthHandlerWrapper(oauth2AuthenticationMechanism)

    val freePlanRelativeProfitCutOff = BigDecimal("0.012")
    val arbitrageProfitController = ArbitrageProfitController(
        twoLegArbitrageProfitOpportunityCache = twoLegArbitrageProfitOpportunityCache,
        orderBookUsdAmountThresholds = appConfig.orderBookUsdAmountThresholds,
        commonExchangeCurrencyPairsService = commonExchangeCurrencyPairsService,
        objectMapper = objectMapper,
        oauth2BearerTokenAuthHandlerWrapper = oauth2BearerTokenAuthHandlerWrapper,
        clientTwoLegArbitrageProfitOpportunities = ClientTwoLegArbitrageProfitOpportunities(freePlanRelativeProfitCutOff),
        freePlanRelativeProfitPercentCutOff = freePlanRelativeProfitCutOff.movePointRight(2).toPlainString(),
        transactionFeeRatioWhenNotAvailableInMetadata = transactionFeeRatioWhenNotAvailableInMetadata,
    )

    val healthService = HealthService(
        commonExchangeCurrencyPairsService = commonExchangeCurrencyPairsService
    )
    val healthController = HealthController(
        healthService = healthService,
        objectMapper = objectMapper,
    )

    val controllers = listOf(arbitrageProfitController, healthController)

    val server = ServerBuilder(appConfig.appServerPort, controllers, metricsService).build()


    fun start(): SocketAddress {
        logger.info { "Fetching currency pairs from exchanges" }
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.calculateCommonCurrencyPairs()
        logCommonCurrencyPairsBetweenExchangePairs(commonCurrencyPairs.exchangePairsToCurrencyPairs)

        val twoLegArbitrageMonitors = twoLegArbitrageMonitorProvider.getTwoLegArbitrageOpportunitiesMonitors(commonCurrencyPairs.currencyPairsToExchangePairs)

        orderBookListeners.prepareOrderBookListeners(twoLegArbitrageMonitors)
        healthService.addOrderBookListenersTo(orderBookListeners)
        tickerListeners.prepareTickerListeners(twoLegArbitrageMonitors)
        healthService.addTickerListenersTo(tickerListeners)

        orderBookSseStreamService.startListeningOrderBookStream(commonCurrencyPairs)
        tickerSseStreamService.startListeningTickerStream(commonCurrencyPairs)

        logger.info { "Scheduling jobs" }
        orderBookSseStreamService.scheduleReconnectOnFailure(commonCurrencyPairs)
        tickerSseStreamService.scheduleReconnectOnFailure(commonCurrencyPairs)
        healthMetricsScheduler.scheduleSendingMetrics()
        twoLegOrderBookArbitrageProfitCacheScheduler.scheduleRemovingTooOldAndSendingMetrics()

        logger.info { "Starting server" }
        server.start()
        return server.listenerInfo[0].address
    }

    private fun logCommonCurrencyPairsBetweenExchangePairs(exchangePairToCurrencyPairs: Map<ExchangePair, Set<CurrencyPair>>) {
        appConfig.exchangesToMonitorTwoLegArbitrageOpportunities.forEachIndexed { index, supportedExchange ->
            for (i in index + 1 until appConfig.exchangesToMonitorTwoLegArbitrageOpportunities.size) {
                val exchangePair = ExchangePair(
                    firstExchange = supportedExchange,
                    secondExchange = appConfig.exchangesToMonitorTwoLegArbitrageOpportunities[i]
                )
                val currencyPairs = exchangePairToCurrencyPairs[exchangePair]
                logger.info { "Number common of currency pairs for $exchangePair = ${currencyPairs?.size ?: 0}" }
            }
        }
    }

}
