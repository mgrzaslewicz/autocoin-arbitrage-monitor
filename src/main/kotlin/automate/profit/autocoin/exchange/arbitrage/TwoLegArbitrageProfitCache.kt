package automate.profit.autocoin.exchange.arbitrage

import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import mu.KLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class TwoLegArbitrageProfitCache(private val maximumTwoLegArbitrageProfitsToKeep: Int) {
    private val profits = ConcurrentHashMap<CurrencyPairWithExchangePair, LinkedList<TwoLegArbitrageProfit>>()

    companion object : KLogging()

    fun addProfit(profit: TwoLegArbitrageProfit?) {
        if (profit != null) {
            val key = CurrencyPairWithExchangePair(profit.currencyPair, profit.exchangePair)
            val list = if (profits.containsKey(key)) {
                profits[key]!!
            } else {
                val newList = LinkedList<TwoLegArbitrageProfit>()
                profits[key] = newList
                newList
            }
            logger.info { "Adding profit $profit" }
            list.addFirst(profit)
            deleteLastItemsIfListTooBig(list)
        }
    }

    private fun deleteLastItemsIfListTooBig(list: LinkedList<TwoLegArbitrageProfit>) {
        while (list.isNotEmpty() && list.size > maximumTwoLegArbitrageProfitsToKeep) {
            list.removeLast()
        }
    }

    fun getProfits(currencyPairWithExchangePair: CurrencyPairWithExchangePair): List<TwoLegArbitrageProfit> {
        return profits.getValue(currencyPairWithExchangePair)
    }

    fun getCurrencyPairWithExchangePairs() = profits.keys.toList()
}