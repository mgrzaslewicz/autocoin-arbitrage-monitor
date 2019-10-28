package automate.profit.autocoin.config

import automate.profit.autocoin.exchange.DefaultTickerListenerRegistrarProvider
import automate.profit.autocoin.exchange.TickerFetchScheduler
import automate.profit.autocoin.exchange.TwoLegArbitrageMonitor
import automate.profit.autocoin.exchange.ticker.DefaultTickerListenerRegistrars
import automate.profit.autocoin.oauth.AccessTokenAuthenticator
import automate.profit.autocoin.oauth.AccessTokenInterceptor
import automate.profit.autocoin.oauth.ClientCredentialsAccessTokenProvider
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
    val twoLegArbitrageMonitors = appConfig.twoLegArbitragePairs.flatMap {
        it.value.map { exchangePair -> TwoLegArbitrageMonitor(it.key, exchangePair) }
    }
    val tickerListeners = twoLegArbitrageMonitors.flatMap { it.getTickerListeners() }
    val tickerFetchScheduler = TickerFetchScheduler(tickerListenerRegistrars)

}