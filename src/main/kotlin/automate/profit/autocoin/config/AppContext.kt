package automate.profit.autocoin.config

import automate.profit.autocoin.api.ArbitrageProfitController
import automate.profit.autocoin.api.ArbitrageProfitStatisticsController
import automate.profit.autocoin.api.ServerBuilder
import automate.profit.autocoin.exchange.DefaultTickerListenerRegistrarProvider
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitCache
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitCalculator
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCache
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCalculator
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.exchange.metadata.RestExchangeMetadataService
import automate.profit.autocoin.exchange.ticker.DefaultTickerListenerRegistrars
import automate.profit.autocoin.exchange.ticker.FileTickerPairRepository
import automate.profit.autocoin.exchange.ticker.TickerListenersProvider
import automate.profit.autocoin.exchange.ticker.TickerPairCache
import automate.profit.autocoin.oauth.*
import automate.profit.autocoin.scheduled.ArbitrageProfitStatisticsCalculateScheduler
import automate.profit.autocoin.scheduled.TickerFetchScheduler
import automate.profit.autocoin.scheduled.TickerPairsSaveScheduler
import okhttp3.OkHttpClient
import java.util.concurrent.Executors

class AppContext(appConfig: AppConfig) {
    val httpClient = OkHttpClient()
    val objectMapper = ObjectMapperProvider().createObjectMapper()
    val accessTokenProvider = ClientCredentialsAccessTokenProvider(httpClient, objectMapper, appConfig)
    val accessTokenAuthenticator = AccessTokenAuthenticator(accessTokenProvider)
    val accessTokenInterceptor = AccessTokenInterceptor(accessTokenProvider)
    val oauth2HttpClient = OkHttpClient.Builder()
            .authenticator(accessTokenAuthenticator)
            .addInterceptor(accessTokenInterceptor)
            .build()
    val tickerListenerRegistrarProvider = DefaultTickerListenerRegistrarProvider(appConfig.tickerApiUrl, oauth2HttpClient, objectMapper)
    val tickerListenerRegistrars = DefaultTickerListenerRegistrars(
            initialTickerListenerRegistrarList = emptyList(),
            tickerListenerRegistrarProvider = tickerListenerRegistrarProvider
    )
    val tickerPairCache = TickerPairCache()
    val twoLegArbitrageProfitCalculator: TwoLegArbitrageProfitCalculator = TwoLegArbitrageProfitCalculator()
    val twoLegArbitrageProfitCache = TwoLegArbitrageProfitCache(appConfig.ageOfOldestTwoLegArbitrageProfitToKeepMs)
    val scheduledExecutorService = Executors.newScheduledThreadPool(3)
    val tickerFetchScheduler = TickerFetchScheduler(tickerListenerRegistrars, twoLegArbitrageProfitCache, scheduledExecutorService)
    val tickerListenersProvider = TickerListenersProvider(tickerPairCache, twoLegArbitrageProfitCalculator, twoLegArbitrageProfitCache)
    private val exchangeMetadataService = RestExchangeMetadataService(oauth2HttpClient, appConfig.tickerApiUrl, objectMapper)
    val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(
            exchangeMetadataService = exchangeMetadataService,
            exchanges = appConfig.exchangesToMonitorTwoLegArbitrageOpportunities,
            twoLegArbitragePairs = appConfig.twoLegArbitragePairs
    )
    val fileTickerPairRepository = FileTickerPairRepository(appConfig.tickerPairsRepositoryPath, appConfig.ageOfOldestTickerPairToKeepInRepositoryMs)
    val tickerPairsSaveScheduler = TickerPairsSaveScheduler(tickerPairCache, fileTickerPairRepository, scheduledExecutorService)

    val twoLegArbitrageProfitStatisticCalculator = TwoLegArbitrageProfitStatisticsCalculator(fileTickerPairRepository, twoLegArbitrageProfitCalculator)
    val twoLegArbitrageProfitStatisticsCache = TwoLegArbitrageProfitStatisticsCache()
    val arbitrageProfitStatisticCalculateScheduler = ArbitrageProfitStatisticsCalculateScheduler(twoLegArbitrageProfitStatisticCalculator, twoLegArbitrageProfitStatisticsCache, scheduledExecutorService)


    val accessTokenChecker = AccessTokenChecker(httpClient, objectMapper, appConfig)
    val oauth2AuthenticationMechanism = Oauth2AuthenticationMechanism(accessTokenChecker)
    val oauth2BearerTokenAuthHandlerWrapper = Oauth2BearerTokenAuthHandlerWrapper(oauth2AuthenticationMechanism)

    val arbitrageProfitController = ArbitrageProfitController(twoLegArbitrageProfitCache, objectMapper, oauth2BearerTokenAuthHandlerWrapper)
    val arbitrageProfitStatisticsController = ArbitrageProfitStatisticsController(twoLegArbitrageProfitStatisticsCache, objectMapper, oauth2BearerTokenAuthHandlerWrapper)

    val controllers = listOf(arbitrageProfitController, arbitrageProfitStatisticsController)

    val server = ServerBuilder(appConfig.appServerPort, controllers).build()
}