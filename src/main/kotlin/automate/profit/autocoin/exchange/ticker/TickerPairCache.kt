package automate.profit.autocoin.exchange.ticker

import java.util.*

class TickerPairCache {

    private val tickerPairs = mutableMapOf<CurrencyPairWithExchangePair, LinkedList<TickerPair>>()

    fun addTickerPair(currencyPairWithExchangePair: CurrencyPairWithExchangePair, tickerPair: TickerPair) {
        val tickerPairs = if (tickerPairs.containsKey(currencyPairWithExchangePair)) {
            tickerPairs[currencyPairWithExchangePair]
        } else {
            val newListForInsertingAtHead = LinkedList<TickerPair>()
            tickerPairs[currencyPairWithExchangePair] = newListForInsertingAtHead
            newListForInsertingAtHead
        }
        tickerPairs!!.addFirst(tickerPair)
    }

    fun getCurrencyPairWithExchangePairs() = tickerPairs.keys.toList()

    fun getTickerCurrencyPairs(currencyPairWithExchangePair: CurrencyPairWithExchangePair) = tickerPairs.getValue(currencyPairWithExchangePair)

}