package automate.profit.autocoin.exchange.ticker

import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TickerPairCache(private val ageOfOldestTickerPairToKeepMs: Long, private val currentTimeMillis: () -> Long = System::currentTimeMillis) {

    private val tickerPairs = ConcurrentHashMap<CurrencyPairWithExchangePair, LinkedList<TickerPair>>()

    fun addTickerPair(currencyPairWithExchangePair: CurrencyPairWithExchangePair, tickerPair: TickerPair) {
        val currencyTickerPairs = if (tickerPairs.containsKey(currencyPairWithExchangePair)) {
            tickerPairs[currencyPairWithExchangePair]
        } else {
            val newListForInsertingAtHead = LinkedList<TickerPair>()
            tickerPairs[currencyPairWithExchangePair] = newListForInsertingAtHead
            newListForInsertingAtHead
        }
        currencyTickerPairs!!.addFirst(tickerPair)
        removeTooOldTickers(currencyTickerPairs, ageOfOldestTickerPairToKeepMs)
    }

    private fun removeTooOldTickers(currencyTickerPairs: LinkedList<TickerPair>, ageOfOldestTickerPairToKeepMs: Long) {
        val currentTimeMs = currentTimeMillis()
        while (listOf(currencyTickerPairs.last.first.timestamp!!.toEpochMilli(), currencyTickerPairs.last.second.timestamp!!.toEpochMilli()).any { currentTimeMs - it > ageOfOldestTickerPairToKeepMs }) {
            currencyTickerPairs.removeLast()
        }
    }

    fun getCurrencyPairWithExchangePairs() = tickerPairs.keys.toList()

    fun getTickerCurrencyPairs(currencyPairWithExchangePair: CurrencyPairWithExchangePair) = tickerPairs.getValue(currencyPairWithExchangePair)

}