package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.currency.CurrencyPair
import java.util.*

private data class CurrencyPairWithExchangePair(
        val currencyPair: CurrencyPair,
        val exchangePair: ExchangePair
)

class TickerPairCache {

    private val tickerPairs = mutableMapOf<CurrencyPairWithExchangePair, LinkedList<TickerPair>>()

    fun addTickerPair(currencyPair: CurrencyPair, exchangePair: ExchangePair, firstExchangeTicker: Ticker, secondExchangeTicker: Ticker) {
        val key = CurrencyPairWithExchangePair(currencyPair, exchangePair)
        val tickerPairs = if (tickerPairs.containsKey(key)) {
            tickerPairs[key]
        } else {
            val newListForInsertingAtHead = LinkedList<TickerPair>()
            tickerPairs[key] = newListForInsertingAtHead
            newListForInsertingAtHead
        }
        tickerPairs!!.addFirst(TickerPair(firstExchangeTicker, secondExchangeTicker))
    }

}