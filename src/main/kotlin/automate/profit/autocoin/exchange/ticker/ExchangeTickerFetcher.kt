package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair

class ExchangeTickerFetcher(
        private val supportedExchange: SupportedExchange,
        private val tickerFetcher: TickerFetcher
) : UserExchangeTickerService {
    override fun getTicker(currencyPair: CurrencyPair): Ticker {
        return tickerFetcher.fetchTicker(supportedExchange, currencyPair)
    }
}