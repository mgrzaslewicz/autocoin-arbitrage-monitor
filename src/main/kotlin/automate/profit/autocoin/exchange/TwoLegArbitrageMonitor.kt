package automate.profit.autocoin.exchange

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerListener

class TwoLegArbitrageMonitor(
        private val currencyPair: CurrencyPair,
        private val exchangePair: ExchangePair,
        private val tickerSpreadCache: TickerSpreadCache
) {
    private var lastFirstExchangeTicker: Ticker? = null
    private var lastSecondExchangeTicker: Ticker? = null

    private fun onFirstExchangeTicker(ticker: Ticker) {
        lastFirstExchangeTicker = ticker
        saveTickerSpreads()
    }

    private fun onSecondExchangeTicker(ticker: Ticker) {
        lastSecondExchangeTicker = ticker
        saveTickerSpreads()
    }

    private fun saveTickerSpreads() {
        if (lastFirstExchangeTicker != null && lastSecondExchangeTicker != null) {
            tickerSpreadCache.addTickerSpread(currencyPair, exchangePair, lastFirstExchangeTicker!!, lastSecondExchangeTicker!!)
        }
    }

    fun getTickerListeners(): Pair<TickerListener, TickerListener> = Pair(
            object : TickerListener {
                override fun currencyPair() = currencyPair
                override fun exchange() = exchangePair.firstExchange

                override fun onNoNewTicker(ticker: Ticker?) {
                    if (ticker != null) onFirstExchangeTicker(ticker)
                }

                override fun onTicker(ticker: Ticker) {
                    onFirstExchangeTicker(ticker)
                }
            },
            object : TickerListener {
                override fun currencyPair() = currencyPair
                override fun exchange() = exchangePair.secondExchange

                override fun onNoNewTicker(ticker: Ticker?) {
                    if (ticker != null) onFirstExchangeTicker(ticker)
                }

                override fun onTicker(ticker: Ticker) {
                    onSecondExchangeTicker(ticker)
                }
            }
    )

}