package automate.profit.autocoin.exchange.arbitrage.ticker

import automate.profit.autocoin.exchange.ticker.*
import mu.KLogging

/**
 * Calculates arbitrage opportunities based on tickers
 */
class TwoLegTickerArbitrageMonitor(
        private val currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        private val tickerPairCache: TickerPairCache,
        private val twoLegTickerArbitrageProfitCalculator: TwoLegTickerArbitrageProfitCalculator,
        private val twoLegTickerArbitrageProfitCache: TwoLegTickerArbitrageProfitCache
) {
    companion object : KLogging()

    private val currencyPair = currencyPairWithExchangePair.currencyPair
    private val exchangePair = currencyPairWithExchangePair.exchangePair
    private var lastFirstExchangeTicker: Ticker? = null
    private var lastSecondExchangeTicker: Ticker? = null

    private fun onFirstExchangeTicker(ticker: Ticker) {
        lastFirstExchangeTicker = ticker
        onTickerPairs()
    }

    private fun onSecondExchangeTicker(ticker: Ticker) {
        lastSecondExchangeTicker = ticker
        onTickerPairs()
    }

    private fun onTickerPairs() {
        if (lastFirstExchangeTicker != null && lastSecondExchangeTicker != null) {
            val tickerPair = TickerPair(lastFirstExchangeTicker!!, lastSecondExchangeTicker!!)
            tickerPairCache.addTickerPair(currencyPairWithExchangePair, tickerPair)
            val profit = twoLegTickerArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, tickerPair)
            if (profit == null) {
                twoLegTickerArbitrageProfitCache.removeProfit(currencyPairWithExchangePair)
            } else {
                twoLegTickerArbitrageProfitCache.addProfit(profit)
            }
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