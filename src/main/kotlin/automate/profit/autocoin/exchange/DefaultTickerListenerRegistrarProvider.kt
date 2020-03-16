package automate.profit.autocoin.exchange

import automate.profit.autocoin.exchange.ticker.*
import java.util.concurrent.ExecutorService

class DefaultTickerListenerRegistrarProvider(
        private val tickerFetcher: TickerFetcher,
        private val executorService: ExecutorService
) : TickerListenerRegistrarProvider {
    override fun createTickerListenerRegistrar(exchangeName: SupportedExchange): TickerListenerRegistrar {
        return DefaultTickerListenerRegistrar(exchangeName, ExchangeTickerFetcher(supportedExchange = exchangeName, tickerFetcher = tickerFetcher), executorService)
    }
}