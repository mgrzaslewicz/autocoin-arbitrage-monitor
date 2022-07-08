package automate.profit.autocoin.config

import automate.profit.autocoin.api.ArbitrageProfitController
import automate.profit.autocoin.api.ArbitrageProfitStatisticsController
import automate.profit.autocoin.api.ServerBuilder
import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.arbitrage.orderbook.FileOrderBookArbitrageProfitRepository
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCalculator
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCache
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.exchange.metadata.RestExchangeMetadataService
import automate.profit.autocoin.exchange.orderbook.DefaultOrderBookListenerRegistrarProvider
import automate.profit.autocoin.exchange.orderbook.DefaultOrderBookListenerRegistrars
import automate.profit.autocoin.exchange.orderbook.OrderBookListenersProvider
import automate.profit.autocoin.exchange.ticker.TickerFetcher
import automate.profit.autocoin.metrics.MetricsService
import automate.profit.autocoin.oauth.client.AccessTokenAuthenticator
import automate.profit.autocoin.oauth.client.AccessTokenInterceptor
import automate.profit.autocoin.oauth.client.ClientCredentialsAccessTokenProvider
import automate.profit.autocoin.oauth.server.AccessTokenChecker
import automate.profit.autocoin.oauth.server.Oauth2AuthenticationMechanism
import automate.profit.autocoin.oauth.server.Oauth2BearerTokenAuthHandlerWrapper
import automate.profit.autocoin.scheduled.ArbitrageProfitStatisticsCalculateScheduler
import automate.profit.autocoin.scheduled.MetricsScheduler
import automate.profit.autocoin.scheduled.OrderBookFetchScheduler
import com.timgroup.statsd.NoOpStatsDClient
import com.timgroup.statsd.NonBlockingStatsDClient
import mu.KLogging
import okhttp3.OkHttpClient
import java.net.SocketAddress
import java.util.concurrent.Executors

class AppContext(private val appConfig: AppConfig) {
    companion object : KLogging()

    val httpClientWithoutAuthorization = OkHttpClient()
    val objectMapper = ObjectMapperProvider().createObjectMapper()
    val accessTokenProvider = ClientCredentialsAccessTokenProvider(
            httpClient = httpClientWithoutAuthorization,
            objectMapper = objectMapper,
            appConfig = appConfig
    )
    val accessTokenAuthenticator = AccessTokenAuthenticator(accessTokenProvider)
    val accessTokenInterceptor = AccessTokenInterceptor(accessTokenProvider)
    val oauth2HttpClient = OkHttpClient.Builder()
            .authenticator(accessTokenAuthenticator)
            .addInterceptor(accessTokenInterceptor)
            .build()
    val tickerFetcher = TickerFetcher(
            tickerApiUrl = appConfig.exchangeMediatorApiUrl,
            httpClient = oauth2HttpClient,
            objectMapper = objectMapper
    )

    val statsdClient =
            if (appConfig.useMetrics)
                NonBlockingStatsDClient(appConfig.serviceName, "localhost", 8125)
            else
                NoOpStatsDClient()
    val metricsService: MetricsService = MetricsService(statsdClient)

    val priceService = PriceService(
            priceApiUrl = appConfig.exchangeMediatorApiUrl,
            httpClient = oauth2HttpClient,
            objectMapper = objectMapper
    )

    val twoLegOrderBookArbitrageProfitCalculator = TwoLegOrderBookArbitrageProfitCalculator(
            priceService = priceService,
            tickerFetcher = tickerFetcher,
            orderBookUsdAmountThresholds = appConfig.orderBookUsdAmountThresholds
    )

    val twoLegOrderBookArbitrageProfitCache = TwoLegOrderBookArbitrageProfitCache(appConfig.ageOfOldestTwoLegArbitrageProfitToKeepInCacheMs)
    val scheduledExecutorService = Executors.newScheduledThreadPool(3)
    val exchangeMetadataService = RestExchangeMetadataService(
            httpClient = oauth2HttpClient,
            exchangeMetadataServiceHostWithPort = appConfig.exchangeMediatorApiUrl,
            objectMapper = objectMapper
    )

    val arbitrageProfitRepository = FileOrderBookArbitrageProfitRepository(
            tickerRepositoryPath = appConfig.profitsRepositoryPath,
            ageOfOldestProfitToKeepMs = appConfig.ageOfOldestTwoLegArbitrageProfitToKeepInRepositoryMs,
            objectMapper = objectMapper
    )

    private val cachedThreadPool = Executors.newCachedThreadPool()

    val orderBookListenersProvider = OrderBookListenersProvider(
            profitCache = twoLegOrderBookArbitrageProfitCache,
            profitCalculator = twoLegOrderBookArbitrageProfitCalculator,
            metricsService = metricsService,
            arbitrageProfitRepository = arbitrageProfitRepository,
            executorService = Executors.newSingleThreadExecutor()
    )

    val orderBookListenerRegistrarProvider = DefaultOrderBookListenerRegistrarProvider(
            orderBookApiUrl = appConfig.exchangeMediatorApiUrl,
            httpClient = oauth2HttpClient,
            objectMapper = objectMapper
    )
    val orderBookListenerRegistrars = DefaultOrderBookListenerRegistrars(
            initialTickerListenerRegistrarList = emptyList(),
            orderBookListenerRegistrarProvider = orderBookListenerRegistrarProvider,
            executorService = cachedThreadPool
    )
    val orderBookFetchScheduler = OrderBookFetchScheduler(
            orderBookListenerRegistrars = orderBookListenerRegistrars,
            twoLegArbitrageProfitCache = twoLegOrderBookArbitrageProfitCache,
            executorService = scheduledExecutorService
    )
    val metricsScheduler = MetricsScheduler(
            metricsService = metricsService,
            executorService = scheduledExecutorService
    )

    val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(
            exchangeMetadataService = exchangeMetadataService,
            exchanges = appConfig.exchangesToMonitorTwoLegArbitrageOpportunities,
            currencyPairsWhiteList = appConfig.arbitrageCurrencyPairsWhiteList,
            twoLegArbitrageCurrencyAndExchangePairs = appConfig.twoLegArbitrageCurrencyAndExchangePairs
    )

    val twoLegArbitrageProfitStatisticCalculator = TwoLegArbitrageProfitStatisticsCalculator(
            profitRepository = arbitrageProfitRepository,
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
        logSharedCurrencyPairsBetweenExchangePairs(commonCurrencyPairs.exchangePairsToCurrencyPairs)

        val orderBookListeners = orderBookListenersProvider.createOrderBookListenersFrom(commonCurrencyPairs.currencyPairsToExchangePairs)
        logger.info { "Registering ${orderBookListeners.size} order book listeners" }
        orderBookListeners.forEach { orderBookListenerRegistrars.registerOrderBookListener(it) }

        logger.info { "Scheduling jobs" }
        orderBookFetchScheduler.scheduleFetchingOrderBooks()

        logger.info { "Scheduling calculating arbitrage profit statistics" }
        arbitrageProfitStatisticsCalculateScheduler.scheduleCacheRefresh()

        logger.info { "Scheduling periodic metrics collection: health, memory and descriptors" }
        metricsScheduler.reportHealth()
        metricsScheduler.reportMemoryUsage()
        metricsScheduler.reportDescriptorsUsage()

        logger.info { "Starting server" }
        server.start()
        return server.listenerInfo[0].address
    }

    private fun logSharedCurrencyPairsBetweenExchangePairs(exchangePairToCurrencyPairs: Map<ExchangePair, Set<CurrencyPair>>) {
        appConfig.exchangesToMonitorTwoLegArbitrageOpportunities.forEachIndexed { index, supportedExchange ->
            for (i in index + 1 until appConfig.exchangesToMonitorTwoLegArbitrageOpportunities.size) {
                val exchangePair = ExchangePair(
                        firstExchange = supportedExchange,
                        secondExchange = appConfig.exchangesToMonitorTwoLegArbitrageOpportunities[i]
                )
                val currencyPairs = exchangePairToCurrencyPairs[exchangePair]
                logger.info { "Number of currency pairs for $exchangePair = ${currencyPairs?.size ?: 0}" }
            }
        }
    }

}