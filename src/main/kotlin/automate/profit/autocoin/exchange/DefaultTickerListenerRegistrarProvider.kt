package automate.profit.autocoin.exchange

import automate.profit.autocoin.exchange.ticker.DefaultTickerListenerRegistrar
import automate.profit.autocoin.exchange.ticker.TickerListenerRegistrar
import automate.profit.autocoin.exchange.ticker.TickerListenerRegistrarProvider
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient

class DefaultTickerListenerRegistrarProvider(private val tickerApiUrl: String, private val httpClient: OkHttpClient, private val objectMapper: ObjectMapper) : TickerListenerRegistrarProvider {
    override fun createTickerListenerRegistrar(exchangeName: SupportedExchange): TickerListenerRegistrar {
        return DefaultTickerListenerRegistrar(exchangeName, ExchangeTickerFetcher(supportedExchange = exchangeName, tickerApiUrl = tickerApiUrl, httpClient = httpClient, objectMapper = objectMapper))
    }
}