package automate.profit.autocoin.exchange

import automate.profit.autocoin.exchange.ticker.*

class DefaultTickerListenerRegistrarProvider(private val tickerFetcher: TickerFetcher) : TickerListenerRegistrarProvider {
    override fun createTickerListenerRegistrar(exchangeName: SupportedExchange): TickerListenerRegistrar {
        return DefaultTickerListenerRegistrar(exchangeName, ExchangeTickerFetcher(supportedExchange = exchangeName, tickerFetcher = tickerFetcher))
    }
}