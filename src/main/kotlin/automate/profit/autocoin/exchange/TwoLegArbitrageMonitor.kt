package automate.profit.autocoin.exchange

import automate.profit.autocoin.exchange.ticker.*

class TwoLegArbitrageMonitor(
        private val currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        private val tickerPairCache: TickerPairCache
) {
    private val currencyPair = currencyPairWithExchangePair.currencyPair
    private val exchangePair = currencyPairWithExchangePair.exchangePair
    private var lastFirstExchangeTicker: Ticker? = null
    private var lastSecondExchangeTicker: Ticker? = null

    private fun onFirstExchangeTicker(ticker: Ticker) {
        lastFirstExchangeTicker = ticker
        saveTickerPairs()
    }

    private fun onSecondExchangeTicker(ticker: Ticker) {
        lastSecondExchangeTicker = ticker
        saveTickerPairs()
    }

    private fun saveTickerPairs() {
        if (lastFirstExchangeTicker != null && lastSecondExchangeTicker != null) {
            tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(lastFirstExchangeTicker!!, lastSecondExchangeTicker!!))
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