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
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.exchange.metadata.RestExchangeMetadataService
import automate.profit.autocoin.exchange.orderbook.DefaultOrderBookListenerRegistrarProvider
import automate.profit.autocoin.exchange.orderbook.DefaultOrderBookListenerRegistrars
import automate.profit.autocoin.exchange.orderbook.OrderBookListenersProvider
import automate.profit.autocoin.exchange.ticker.TickerFetcher
import automate.profit.autocoin.metrics.FileStatsdClient
import automate.profit.autocoin.metrics.Oauth2MetricsHandlerWrapper
import automate.profit.autocoin.oauth.client.AccessTokenAuthenticator
import automate.profit.autocoin.oauth.client.AccessTokenInterceptor
import automate.profit.autocoin.oauth.client.ClientCredentialsAccessTokenProvider
import automate.profit.autocoin.oauth.server.AccessTokenChecker
import automate.profit.autocoin.oauth.server.Oauth2AuthenticationMechanism
import automate.profit.autocoin.oauth.server.Oauth2BearerTokenAuthHandlerWrapper
import automate.profit.autocoin.scheduled.ArbitrageProfitStatisticsCalculateScheduler
import automate.profit.autocoin.scheduled.MetricsSaveScheduler
import automate.profit.autocoin.scheduled.OrderBookFetchScheduler
import mu.KLogging
import okhttp3.OkHttpClient
import java.util.concurrent.Executors

class AppContext(appConfig: AppConfig) {
    companion object : KLogging()

    val httpClientWithoutAuthorization = OkHttpClient()
    val objectMapper = ObjectMapperProvider().createObjectMapper()
    val accessTokenProvider = ClientCredentialsAccessTokenProvider(httpClientWithoutAuthorization, objectMapper, appConfig)
    val accessTokenAuthenticator = AccessTokenAuthenticator(accessTokenProvider)
    val accessTokenInterceptor = AccessTokenInterceptor(accessTokenProvider)
    val oauth2HttpClient = OkHttpClient.Builder()
            .authenticator(accessTokenAuthenticator)
            .addInterceptor(accessTokenInterceptor)
            .build()
    val tickerFetcher = TickerFetcher(appConfig.exchangeMediatorApiUrl, oauth2HttpClient, objectMapper)

    private val statsdClient = FileStatsdClient(appConfig.metricsFolder)

    val priceService = PriceService(appConfig.exchangeMediatorApiUrl, oauth2HttpClient, objectMapper, statsdClient)

    val twoLegOrderBookArbitrageProfitCalculator: TwoLegOrderBookArbitrageProfitCalculator = TwoLegOrderBookArbitrageProfitCalculator(priceService, tickerFetcher, appConfig.orderBookUsdAmountThresholds)

    val twoLegOrderBookArbitrageProfitCache = TwoLegOrderBookArbitrageProfitCache(appConfig.ageOfOldestTwoLegArbitrageProfitToKeepInCacheMs)
    val scheduledExecutorService = Executors.newScheduledThreadPool(3)
    private val exchangeMetadataService = RestExchangeMetadataService(oauth2HttpClient, appConfig.exchangeMediatorApiUrl, objectMapper)

    val arbitrageProfitRepository = FileOrderBookArbitrageProfitRepository(appConfig.profitsRepositoryPath, appConfig.ageOfOldestTwoLegArbitrageProfitToKeepInRepositoryMs, objectMapper)
    val orderBookListenersProvider = OrderBookListenersProvider(twoLegOrderBookArbitrageProfitCache, twoLegOrderBookArbitrageProfitCalculator, statsdClient, arbitrageProfitRepository)
    val orderBookListenerRegistrarProvider = DefaultOrderBookListenerRegistrarProvider(appConfig.exchangeMediatorApiUrl, oauth2HttpClient, objectMapper, statsdClient)
    val orderBookListenerRegistrars = DefaultOrderBookListenerRegistrars(
            initialTickerListenerRegistrarList = emptyList(),
            orderBookListenerRegistrarProvider = orderBookListenerRegistrarProvider
    )
    val orderBookFetchScheduler = OrderBookFetchScheduler(orderBookListenerRegistrars, twoLegOrderBookArbitrageProfitCache, scheduledExecutorService)
    val metricsSaveScheduler = MetricsSaveScheduler(statsdClient, scheduledExecutorService, appConfig.saveMetricsToFileEveryNSeconds)

    val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(
            exchangeMetadataService = exchangeMetadataService,
            exchanges = appConfig.exchangesToMonitorTwoLegArbitrageOpportunities,
            currencyPairsWhiteList = appConfig.arbitrageCurrencyPairsWhiteList,
            twoLegArbitrageCurrencyAndExchangePairs = appConfig.twoLegArbitrageCurrencyAndExchangePairs
    )

    val twoLegArbitrageProfitStatisticCalculator = TwoLegArbitrageProfitStatisticsCalculator(twoLegOrderBookArbitrageProfitCalculator)
    val twoLegArbitrageProfitStatisticsCache = TwoLegArbitrageProfitStatisticsCache()
    val arbitrageProfitStatisticsCalculateScheduler = ArbitrageProfitStatisticsCalculateScheduler(twoLegArbitrageProfitStatisticCalculator, twoLegArbitrageProfitStatisticsCache, scheduledExecutorService)


    val accessTokenChecker = AccessTokenChecker(httpClientWithoutAuthorization, objectMapper, appConfig)
    val oauth2AuthenticationMechanism = Oauth2AuthenticationMechanism(accessTokenChecker)
    val oauth2BearerTokenAuthHandlerWrapper = Oauth2BearerTokenAuthHandlerWrapper(oauth2AuthenticationMechanism)

    val oauth2MetricsHandlerWrapper = Oauth2MetricsHandlerWrapper(statsdClient)
    val arbitrageProfitController = ArbitrageProfitController(twoLegOrderBookArbitrageProfitCache, appConfig.orderBookUsdAmountThresholds, objectMapper, oauth2BearerTokenAuthHandlerWrapper, oauth2MetricsHandlerWrapper)
    val arbitrageProfitStatisticsController = ArbitrageProfitStatisticsController(twoLegArbitrageProfitStatisticsCache, objectMapper, oauth2BearerTokenAuthHandlerWrapper, oauth2MetricsHandlerWrapper)

    val controllers = listOf(arbitrageProfitController, arbitrageProfitStatisticsController)

    val server = ServerBuilder(appConfig.appServerPort, controllers).build()

    fun start() {
        logger.info { "Fetching currency pairs from exchanges" }
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.getCommonCurrencyPairs()

        val orderBookListeners = orderBookListenersProvider.createOrderBookListenersFrom(commonCurrencyPairs)
        logger.info { "Registering ${orderBookListeners.size} order book listeners" }
        orderBookListeners.forEach { orderBookListenerRegistrars.registerOrderBookListener(it) }

        logger.info { "Scheduling jobs" }
        orderBookFetchScheduler.scheduleFetchingOrderBooks()
        metricsSaveScheduler.scheduleSavingMetrics()

        logger.info { "Scheduling calculating arbitrage profit statistics" }
        arbitrageProfitStatisticsCalculateScheduler.scheduleCacheRefresh()
        logger.info { "Starting server" }
        server.start()
    }

}