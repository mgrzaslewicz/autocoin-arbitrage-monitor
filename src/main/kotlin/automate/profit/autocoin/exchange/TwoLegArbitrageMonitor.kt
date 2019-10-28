package automate.profit.autocoin.exchange

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerListener

class TwoLegArbitrageMonitor(private val currencyPair: CurrencyPair, private val exchangePair: ExchangePair) {

    private fun onFirstExchangeTicker(ticker: Ticker) {

    }

    private fun onSecondExchangeTicker(ticker: Ticker) {

    }

    fun getTickerListeners(): List<TickerListener> = listOf(
            object : TickerListener {
                override fun currencyPair() = currencyPair
                override fun exchange() = exchangePair.firstExchange
                override fun onTicker(ticker: Ticker) {
                    onFirstExchangeTicker(ticker)
                }
            },
            object : TickerListener {
                override fun currencyPair() = currencyPair
                override fun exchange() = exchangePair.secondExchange
                override fun onTicker(ticker: Ticker) {
                    onSecondExchangeTicker(ticker)
                }
            }
    )

}