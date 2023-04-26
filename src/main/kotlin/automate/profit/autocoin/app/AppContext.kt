package automate.profit.autocoin.app

import autocoin.metrics.JsonlFileStatsDClient
import automate.profit.autocoin.api.ArbitrageProfitController
import automate.profit.autocoin.api.ClientTwoLegArbitrageProfitOpportunities
import automate.profit.autocoin.api.HealthController
import automate.profit.autocoin.api.ServerBuilder
import automate.profit.autocoin.api.health.HealthService
import automate.profit.autocoin.app.config.AppConfig
import automate.profit.autocoin.exchange.CachingPriceService
import automate.profit.autocoin.exchange.RestPriceService
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitOpportunitiesMonitorsProvider
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitCalculatorWithMetadata
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCalculator
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCutOff
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
import java.nio.file.Path
import java.time.Duration
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class AppContext(val appConfig: AppConfig) {
    private companion object : KLogging()

    private val instanceId: String = UUID.randomUUID().toString()

    val httpClientWithoutAuthorization = OkHttpClient()
    val objectMapper = ObjectMapperProvider().createObjectMapper()
    val accessTokenProvider = ClientCredentialsAccessTokenProvider(
        httpClient = httpClientWithoutAuthorization,
        objectMapper = objectMapper,
        oauth2ServerUrl = appConfig.oauth2ApiBaseUrl,
        oauthClientId = appConfig.arbitrageMonitorOauth2ClientId,
        oauthClientSecret = appConfig.arbitrageMonitorOauth2ClientSecret
    )
    val accessTokenAuthenticator = AccessTokenAuthenticator(accessTokenProvider)
    val accessTokenInterceptor = AccessTokenInterceptor(accessTokenProvider)
    val oauth2HttpClient = OkHttpClient.Builder()
        .authenticator(accessTokenAuthenticator)
        .addInterceptor(accessTokenInterceptor)
        .callTimeout(15, TimeUnit.SECONDS)
        .build()
    val sseHttpClient = oauth2HttpClient.newBuilder()
        .readTimeout(Duration.ofMillis(0L))
        .build()
    val sseEventSourceFactory = EventSources.createFactory(sseHttpClient)

    val statsdClient = if (appConfig.useRealStatsDClient) {
        NonBlockingStatsDClient(appConfig.serviceName, appConfig.telegrafHostname, 8125)
    } else {
        val metricsFolderPath = Path.of(appConfig.metricsFolder)
        metricsFolderPath.toFile().mkdirs()
        val metricsFile = metricsFolderPath.resolve("metrics.jsonl")
        logger.warn { "Using JsonlFileStatsDClient as TELEGRAF_HOSTNAME set to 'metrics.jsonl'. Writing metrics to ${metricsFile.toAbsolutePath()}" }
        JsonlFileStatsDClient(metricsFile.toFile())
    }
    val metricsService: MetricsService = MetricsService(statsdClient)

    val scheduledJobsxecutorService = Executors.newScheduledThreadPool(3)

    val priceService = CachingPriceService(
        decorated = RestPriceService(
            priceApiUrl = appConfig.exchangeMediatorApiBaseUrl,
            httpClient = oauth2HttpClient,
            metricsService = metricsService,
            objectMapper = objectMapper
        ),
        executorService = scheduledJobsxecutorService,
    )

    val exchangeMetadataService = CachingExchangeMetadataService(
        decorated = RestExchangeMetadataService(
            httpClient = oauth2HttpClient,
            exchangeMetadataApiBaseurl = appConfig.exchangeMediatorApiBaseUrl,
            objectMapper = objectMapper
        )
    )

    private val transactionFeeRatioWhenNotAvailableInMetadata = BigDecimal("0.001")

    val twoLegArbitrageProfitOpportunityCalculatorWithMetadata = TwoLegArbitrageProfitOpportunityCalculator(
        opportunityCutOff = TwoLegArbitrageProfitOpportunityCutOff(),
        priceService = priceService,
        orderBookUsdAmountThresholds = appConfig.orderBookUsdAmountThresholds,
        relativeProfitCalculator = TwoLegArbitrageProfitCalculatorWithMetadata.DefaultBuilder(
            metadataService = exchangeMetadataService,
            transactionFeeRatioWhenNotAvailableInMetadata = transactionFeeRatioWhenNotAvailableInMetadata
        ).build(),
        metricsService = metricsService,
    )

    val twoLegArbitrageProfitOpportunityCache = TwoLegArbitrageProfitOpportunityCache(appConfig.ageOfOldestTwoLegArbitrageProfitToKeepInCacheMs)


    val twoLegOrderBookArbitrageProfitCacheScheduler = TwoLegOrderBookArbitrageProfitCacheScheduler(
        scheduledExecutorService = scheduledJobsxecutorService,
        ageOfOldestTwoLegArbitrageProfitToKeepMs = appConfig.ageOfOldestTwoLegArbitrageProfitToKeepInCacheMs,
        twoLegArbitrageProfitOpportunityCache = twoLegArbitrageProfitOpportunityCache,
    )

    val orderBookListeners = OrderBookListeners()
    val tickerListeners = TickerListeners()
    val twoLegArbitrageMonitorProvider = TwoLegArbitrageProfitOpportunitiesMonitorsProvider(
        profitCache = twoLegArbitrageProfitOpportunityCache,
        profitCalculator = twoLegArbitrageProfitOpportunityCalculatorWithMetadata,
    )

    val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(
        exchangeMetadataService = exchangeMetadataService,
        currencyPairsWhiteList = appConfig.arbitrageCurrencyPairsWhiteList,
        staticTwoLegArbitrageCurrencyAndExchangePairs = appConfig.twoLegArbitrageCurrencyAndExchangePairs
    )

    val threadForStreamReconnecting = Executors.newSingleThreadExecutor()

    val orderBookSseStreamService = OrderBookSseStreamService(
        orderBookApiBaseUrl = appConfig.exchangeMediatorApiBaseUrl,
        httpClient = sseHttpClient,
        eventSourceFactory = sseEventSourceFactory,
        orderBookListeners = orderBookListeners,
        objectMapper = objectMapper,
        executorForReconnecting = threadForStreamReconnecting,
        instanceId = instanceId,
    )
    val tickerSseStreamService = TickerSseStreamService(
        tickerApiBaseUrl = appConfig.exchangeMediatorApiBaseUrl,
        httpClient = sseHttpClient,
        eventSourceFactory = sseEventSourceFactory,
        tickerListeners = tickerListeners,
        objectMapper = objectMapper,
        executorForReconnecting = threadForStreamReconnecting,
        instanceId = instanceId,
    )

    val healthService = HealthService(
        orderBookSseStreamService = orderBookSseStreamService,
        tickerSseStreamService = tickerSseStreamService,
        commonExchangeCurrencyPairsService = commonExchangeCurrencyPairsService,
        twoLegArbitrageProfitOpportunityCache = twoLegArbitrageProfitOpportunityCache,
    )

    val healthMetricsScheduler = HealthMetricsScheduler(
        healthService = healthService,
        metricsService = metricsService,
        executorService = scheduledJobsxecutorService
    )

    val accessTokenChecker = AccessTokenChecker(httpClientWithoutAuthorization, objectMapper, appConfig)
    val oauth2AuthenticationMechanism = Oauth2AuthenticationMechanism(accessTokenChecker)
    val oauth2BearerTokenAuthHandlerWrapper = Oauth2BearerTokenAuthHandlerWrapper(oauth2AuthenticationMechanism)

    val freePlanRelativeProfitCutOff = BigDecimal("0.012")
    val arbitrageProfitController = ArbitrageProfitController(
        exchangesToMonitorTwoLegArbitrageOpportunities = appConfig.exchangesToMonitorTwoLegArbitrageOpportunities,
        twoLegArbitrageProfitOpportunityCache = twoLegArbitrageProfitOpportunityCache,
        orderBookUsdAmountThresholds = appConfig.orderBookUsdAmountThresholds,
        commonExchangeCurrencyPairsService = commonExchangeCurrencyPairsService,
        objectMapper = objectMapper,
        oauth2BearerTokenAuthHandlerWrapper = oauth2BearerTokenAuthHandlerWrapper,
        clientTwoLegArbitrageProfitOpportunities = ClientTwoLegArbitrageProfitOpportunities(
            freePlanRelativeProfitCutOff = freePlanRelativeProfitCutOff,
            exchangeMetadataService = exchangeMetadataService,
        ),
        freePlanRelativeProfitPercentCutOff = freePlanRelativeProfitCutOff.movePointRight(2).toPlainString(),
        transactionFeeRatioWhenNotAvailableInMetadata = transactionFeeRatioWhenNotAvailableInMetadata,
    )

    val healthController = HealthController(
        healthService = healthService,
        objectMapper = objectMapper,
    )

    val controllers = listOf(arbitrageProfitController, healthController)

    val server = ServerBuilder(appConfig.appServerPort, controllers, metricsService).build()


}
