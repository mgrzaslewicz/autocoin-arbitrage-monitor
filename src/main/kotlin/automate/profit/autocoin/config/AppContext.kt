package automate.profit.autocoin.config

import automate.profit.autocoin.api.ArbitrageProfitController
import automate.profit.autocoin.api.ServerBuilder
import automate.profit.autocoin.exchange.DefaultTickerListenerRegistrarProvider
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitCache
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.exchange.metadata.RestExchangeMetadataService
import automate.profit.autocoin.exchange.ticker.*
import automate.profit.autocoin.oauth.*
import automate.profit.autocoin.scheduled.TickerFetchScheduler
import automate.profit.autocoin.scheduled.TickerPairsSaveScheduler
import okhttp3.OkHttpClient

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
    val tickerFetchScheduler = TickerFetchScheduler(tickerListenerRegistrars, twoLegArbitrageProfitCache)
    val tickerListenersProvider = TickerListenersProvider(tickerPairCache, twoLegArbitrageProfitCalculator, twoLegArbitrageProfitCache)
    private val exchangeMetadataService = RestExchangeMetadataService(oauth2HttpClient, appConfig.tickerApiUrl, objectMapper)
    val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(
            exchangeMetadataService = exchangeMetadataService,
            exchanges = appConfig.exchangesToMonitorTwoLegArbitrageOpportunities,
            twoLegArbitragePairs = appConfig.twoLegArbitragePairs
    )
    val fileTickerPairRepository = FileTickerPairRepository(appConfig.tickerPairsRepositoryPath, appConfig.ageOfOldestTickerPairToKeepInRepositoryMs)
    val tickerPairsSaveScheduler = TickerPairsSaveScheduler(tickerPairCache, fileTickerPairRepository)
    val accessTokenChecker = AccessTokenChecker(httpClient, objectMapper, appConfig)
    val oauth2AuthenticationMechanism = Oauth2AuthenticationMechanism(accessTokenChecker)
    val oauth2BearerTokenAuthHandlerWrapper = Oauth2BearerTokenAuthHandlerWrapper(oauth2AuthenticationMechanism)
    val arbitrageProfitController = ArbitrageProfitController(twoLegArbitrageProfitCache, objectMapper, oauth2BearerTokenAuthHandlerWrapper)
    val controllers = listOf(arbitrageProfitController)
    val server = ServerBuilder(appConfig.appServerPort, controllers).build()
}