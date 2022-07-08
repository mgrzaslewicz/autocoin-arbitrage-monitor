package automate.profit.autocoin.exchange.ticker

import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TickerPairCache() {

    private val tickerPairs = ConcurrentHashMap<CurrencyPairWithExchangePair, LinkedList<TickerPair>>()

    fun addTickerPair(currencyPairWithExchangePair: CurrencyPairWithExchangePair, tickerPair: TickerPair) {
        val currencyTickerPairs = if (tickerPairs.containsKey(currencyPairWithExchangePair)) {
            tickerPairs[currencyPairWithExchangePair]
        } else {
            val newListForInsertingAtHead = LinkedList<TickerPair>()
            tickerPairs[currencyPairWithExchangePair] = newListForInsertingAtHead
            newListForInsertingAtHead
        }
        synchronized(this) {
            currencyTickerPairs!!.addFirst(tickerPair)
        }
    }

    fun getCurrencyPairWithExchangePairs() = tickerPairs.keys.toList()

    fun getTickerCurrencyPairs(currencyPairWithExchangePair: CurrencyPairWithExchangePair, maxElements: Int = 0): List<TickerPair> {
        return if (maxElements == 0) {
            tickerPairs.getValue(currencyPairWithExchangePair)
        } else {
            val list = tickerPairs.getValue(currencyPairWithExchangePair)
            list.subList(0, Math.min(maxElements - 1, list.lastIndex))
        }
    }

    fun getAndCleanTickerCurrencyPairs(currencyPairWithExchangePair: CurrencyPairWithExchangePair): List<TickerPair> {
        synchronized(this) {
            val tickerPairListToModify = tickerPairs.getValue(currencyPairWithExchangePair)
            val result = ArrayList(tickerPairListToModify)
            tickerPairListToModify.clear()
            return result
        }
    }

}