package automate.profit.autocoin.config

import automate.profit.autocoin.api.ArbitrageProfitController
import automate.profit.autocoin.api.ArbitrageProfitStatisticsController
import automate.profit.autocoin.api.ServerBuilder
import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.arbitrage.TwoLegOrderBookArbitrageMonitorProvider
import automate.profit.autocoin.exchange.arbitrage.orderbook.OrderBookArbitrageProfitRepository
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfit
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCalculator
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCache
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.exchange.metadata.RestExchangeMetadataService
import automate.profit.autocoin.exchange.orderbook.OrderBookListenersProvider
import automate.profit.autocoin.exchange.orderbookstream.OrderBookSseStreamService
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.TickerListenersProvider
import automate.profit.autocoin.exchange.tickerstream.TickerSseStreamService
import automate.profit.autocoin.metrics.MetricsService
import automate.profit.autocoin.oauth.client.AccessTokenAuthenticator
import automate.profit.autocoin.oauth.client.AccessTokenInterceptor
import automate.profit.autocoin.oauth.client.ClientCredentialsAccessTokenProvider
import automate.profit.autocoin.oauth.server.AccessTokenChecker
import automate.profit.autocoin.oauth.server.Oauth2AuthenticationMechanism
import automate.profit.autocoin.oauth.server.Oauth2BearerTokenAuthHandlerWrapper
import automate.profit.autocoin.scheduled.ArbitrageProfitStatisticsCalculateScheduler
import automate.profit.autocoin.scheduled.MetricsScheduler
import com.timgroup.statsd.NoOpStatsDClient
import com.timgroup.statsd.NonBlockingStatsDClient
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.sse.EventSources
import java.net.SocketAddress
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
        logger.warn { "Using NoOpStatsDClient" }
        NoOpStatsDClient()
    }
    val metricsService: MetricsService = MetricsService(statsdClient)

    val priceService = PriceService(
            priceApiUrl = appConfig.exchangeMediatorApiUrl,
            httpClient = oauth2HttpClient,
            metricsService = metricsService,
            objectMapper = objectMapper
    )

    val twoLegOrderBookArbitrageProfitCalculator = TwoLegOrderBookArbitrageProfitCalculator(
            priceService = priceService,
            orderBookUsdAmountThresholds = appConfig.orderBookUsdAmountThresholds
    )

    val twoLegOrderBookArbitrageProfitCache = TwoLegOrderBookArbitrageProfitCache(appConfig.ageOfOldestTwoLegArbitrageProfitToKeepInCacheMs)
    val scheduledExecutorService = Executors.newScheduledThreadPool(3)
    val exchangeMetadataService = RestExchangeMetadataService(
            httpClient = oauth2HttpClient,
            exchangeMetadataServiceHostWithPort = appConfig.exchangeMediatorApiUrl,
            objectMapper = objectMapper
    )

    val orderBookListenersProvider = OrderBookListenersProvider()
    val tickerListenersProvider = TickerListenersProvider()
    val twoLegArbitrageMonitorProvider = TwoLegOrderBookArbitrageMonitorProvider(
            profitCache = twoLegOrderBookArbitrageProfitCache,
            profitCalculator = twoLegOrderBookArbitrageProfitCalculator,
            metricsService = metricsService
    )

    val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(
            exchangeMetadataService = exchangeMetadataService,
            exchanges = appConfig.exchangesToMonitorTwoLegArbitrageOpportunities,
            currencyPairsWhiteList = appConfig.arbitrageCurrencyPairsWhiteList,
            twoLegArbitrageCurrencyAndExchangePairs = appConfig.twoLegArbitrageCurrencyAndExchangePairs
    )

    val threadForStreamReconnecting = Executors.newSingleThreadExecutor()

    val orderBookSseStreamService = OrderBookSseStreamService(
            orderBookApiBaseUrl = appConfig.exchangeMediatorApiUrl,
            httpClient = sseHttpClient,
            eventSourceFactory = sseEventSourceFactory,
            orderBookListenersProvider = orderBookListenersProvider,
            objectMapper = objectMapper,
            executorForReconnecting = threadForStreamReconnecting
    )
    val tickerSseStreamService = TickerSseStreamService(
            tickerApiBaseUrl = appConfig.exchangeMediatorApiUrl,
            httpClient = sseHttpClient,
            eventSourceFactory = sseEventSourceFactory,
            tickerListenersProvider = tickerListenersProvider,
            objectMapper = objectMapper,
            executorForReconnecting = threadForStreamReconnecting
    )

    val metricsScheduler = MetricsScheduler(
            orderBookSseStreamService = orderBookSseStreamService,
            metricsService = metricsService,
            executorService = scheduledExecutorService
    )

    val twoLegArbitrageProfitStatisticCalculator = TwoLegArbitrageProfitStatisticsCalculator(
            profitRepository = object : OrderBookArbitrageProfitRepository {
                // dummy implementation, it's not a priority right now
                override fun getAllCurrencyPairsWithExchangePairs(): List<CurrencyPairWithExchangePair> = emptyList()
                override fun getProfits(currencyPairWithExchangePair: CurrencyPairWithExchangePair): List<TwoLegOrderBookArbitrageProfit> = emptyList()
            },
            orderBookUsdAmountThresholds = appConfig.orderBookUsdAmountThresholds
    )
    val twoLegArbitrageProfitStatisticsCache = TwoLegArbitrageProfitStatisticsCache()
    val arbitrageProfitStatisticsCalculateScheduler = ArbitrageProfitStatisticsCalculateScheduler(
            twoLegArbitrageProfitStatisticsCalculator = twoLegArbitrageProfitStatisticCalculator,
            twoLegArbitrageProfitStatisticsCache = twoLegArbitrageProfitStatisticsCache,
            executorService = scheduledExecutorService
    )


    val accessTokenChecker = AccessTokenChecker(httpClientWithoutAuthorization, objectMapper, appConfig)
    val oauth2AuthenticationMechanism = Oauth2AuthenticationMechanism(accessTokenChecker)
    val oauth2BearerTokenAuthHandlerWrapper = Oauth2BearerTokenAuthHandlerWrapper(oauth2AuthenticationMechanism)

    val arbitrageProfitController = ArbitrageProfitController(
            twoLegOrderBookArbitrageProfitCache = twoLegOrderBookArbitrageProfitCache,
            orderBookUsdAmountThresholds = appConfig.orderBookUsdAmountThresholds,
            commonExchangeCurrencyPairsService = commonExchangeCurrencyPairsService,
            objectMapper = objectMapper,
            oauth2BearerTokenAuthHandlerWrapper = oauth2BearerTokenAuthHandlerWrapper
    )
    val arbitrageProfitStatisticsController = ArbitrageProfitStatisticsController(
            twoLegArbitrageProfitStatisticsCache = twoLegArbitrageProfitStatisticsCache,
            objectMapper = objectMapper,
            oauth2BearerTokenAuthHandlerWrapper = oauth2BearerTokenAuthHandlerWrapper
    )

    val controllers = listOf(arbitrageProfitController, arbitrageProfitStatisticsController)

    val server = ServerBuilder(appConfig.appServerPort, controllers, metricsService).build()


    fun start(): SocketAddress {
        logger.info { "Fetching currency pairs from exchanges" }
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.calculateCommonCurrencyPairs()
        logCommonCurrencyPairsBetweenExchangePairs(commonCurrencyPairs.exchangePairsToCurrencyPairs)

        val twoLegArbitrageMonitors = twoLegArbitrageMonitorProvider.getTwoLegOrderBookArbitrageMonitors(commonCurrencyPairs.currencyPairsToExchangePairs)

        orderBookListenersProvider.prepareOrderBookListeners(twoLegArbitrageMonitors)
        tickerListenersProvider.prepareTickerListeners(twoLegArbitrageMonitors)
        orderBookSseStreamService.startListeningOrderBookStream(commonCurrencyPairs)
        tickerSseStreamService.startListeningTickerStream(commonCurrencyPairs)

        logger.info { "Scheduling jobs" }
        orderBookSseStreamService.scheduleReconnectOnFailure(commonCurrencyPairs)
        tickerSseStreamService.scheduleReconnectOnFailure(commonCurrencyPairs)
        arbitrageProfitStatisticsCalculateScheduler.scheduleCacheRefresh()
        metricsScheduler.scheduleSendingMetrics()

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