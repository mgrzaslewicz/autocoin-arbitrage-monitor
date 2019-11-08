package automate.profit.autocoin.config

import automate.profit.autocoin.api.ArbitrageProfitController
import automate.profit.autocoin.api.ServerBuilder
import automate.profit.autocoin.exchange.DefaultTickerListenerRegistrarProvider
import automate.profit.autocoin.exchange.TwoLegArbitrageMonitor
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitCache
import automate.profit.autocoin.exchange.ticker.*
import automate.profit.autocoin.oauth.*
import automate.profit.autocoin.scheduled.TickerFetchScheduler
import automate.profit.autocoin.scheduled.TickerPairsSaveScheduler
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import okhttp3.OkHttpClient

class AppContext(val appConfig: AppConfig) {
    val httpClient = OkHttpClient()
    val objectMapper = ObjectMapper().registerModule(KotlinModule())
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
    val tickerPairCache = TickerPairCache(appConfig.ageOfOldestTickerPairToKeepMs)
    val twoLegArbitrageProfitCalculator: TwoLegArbitrageProfitCalculator = TwoLegArbitrageProfitCalculator()
    val twoLegArbitrageProfitCache = TwoLegArbitrageProfitCache(appConfig.ageOfOldestTwoLegArbitrageProfitToKeepMs)
    val twoLegArbitrageMonitors = appConfig.twoLegArbitragePairs.flatMap {
        it.value.map { exchangePair -> TwoLegArbitrageMonitor(CurrencyPairWithExchangePair(it.key, exchangePair), tickerPairCache, twoLegArbitrageProfitCalculator, twoLegArbitrageProfitCache) }
    }
    val tickerListeners = twoLegArbitrageMonitors.flatMap { it.getTickerListeners().toList() }
    val tickerFetchScheduler = TickerFetchScheduler(tickerListenerRegistrars, twoLegArbitrageProfitCache)
    val fileTickerPairRepository = FileTickerPairRepository(appConfig.tickerPairsRepositoryPath)
    val tickerPairsSaveScheduler = TickerPairsSaveScheduler(tickerPairCache, fileTickerPairRepository)
    val tickerPairCacheLoader = TickerPairCacheLoader(tickerPairCache, fileTickerPairRepository)
    val accessTokenChecker = AccessTokenChecker(httpClient, objectMapper, appConfig)
    val oauth2AuthenticationMechanism = Oauth2AuthenticationMechanism(accessTokenChecker)
    val oauth2BearerTokenAuthHandlerWrapper = Oauth2BearerTokenAuthHandlerWrapper(oauth2AuthenticationMechanism)
    val arbitrageProfitController = ArbitrageProfitController(twoLegArbitrageProfitCache, objectMapper, oauth2BearerTokenAuthHandlerWrapper)
    val controllers = listOf(arbitrageProfitController)
    val server = ServerBuilder(appConfig.appServerPort, controllers).build()
}