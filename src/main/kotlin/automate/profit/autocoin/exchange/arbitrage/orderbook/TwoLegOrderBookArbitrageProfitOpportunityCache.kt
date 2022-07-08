package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import mu.KLogging
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import kotlin.collections.HashMap

data class ExchangePairWithOpportunityCount(
    val exchangePair: ExchangePair,
    val opportunityCount: Long,
)

class TwoLegOrderBookArbitrageProfitOpportunityCache(
    private val ageOfOldestTwoLegArbitrageProfitToKeepMs: Long,
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
) {
    private val profitGroups: Map<TwoLegArbitrageRelativeProfitGroup, ConcurrentHashMap<CurrencyPairWithExchangePair, TwoLegOrderBookArbitrageProfit>> =
        TwoLegArbitrageRelativeProfitGroup
            .values()
            .associateWith { ConcurrentHashMap<CurrencyPairWithExchangePair, TwoLegOrderBookArbitrageProfit>() }
    private val noOpportunityCount: Map<TwoLegArbitrageRelativeProfitGroup, MutableMap<CurrencyPairWithExchangePair, Long>> =
        TwoLegArbitrageRelativeProfitGroup
            .values()
            .associateWith { ConcurrentHashMap<CurrencyPairWithExchangePair, Long>() }

    companion object : KLogging()

    fun setProfitOpportunity(profitGroup: TwoLegArbitrageRelativeProfitGroup, profit: TwoLegOrderBookArbitrageProfit) {
        logger.debug { "Setting profit $profit" }
        synchronized(profitGroups[profitGroup]!!) {
            profitGroups[profitGroup]!![profit.currencyPairWithExchangePair] = profit
        }
    }

    fun removeProfitOpportunity(profitGroup: TwoLegArbitrageRelativeProfitGroup, currencyPairWithExchangePair: CurrencyPairWithExchangePair) {
        synchronized(profitGroups[profitGroup]!!) {
            if (logger.isDebugEnabled) {
                if (profitGroups[profitGroup]!!.contains(currencyPairWithExchangePair)) {
                    logger.debug("Removing profit for key $currencyPairWithExchangePair")
                }
            }
            profitGroups[profitGroup]!!.remove(currencyPairWithExchangePair)
        }
        countNoOpportunityFound(profitGroup, currencyPairWithExchangePair)
    }

    private fun countNoOpportunityFound(profitGroup: TwoLegArbitrageRelativeProfitGroup, currencyPairWithExchangePair: CurrencyPairWithExchangePair) {
        synchronized(noOpportunityCount[profitGroup]!!) {
            if (!noOpportunityCount[profitGroup]!!.containsKey(currencyPairWithExchangePair)) {
                noOpportunityCount[profitGroup]!![currencyPairWithExchangePair] = 0L
            }
            noOpportunityCount[profitGroup]!![currencyPairWithExchangePair] = noOpportunityCount[profitGroup]!![currencyPairWithExchangePair]!! + 1
        }
    }

    fun getProfit(profitGroup: TwoLegArbitrageRelativeProfitGroup, currencyPairWithExchangePair: CurrencyPairWithExchangePair): TwoLegOrderBookArbitrageProfit? {
        return profitGroups[profitGroup]!![currencyPairWithExchangePair]
    }

    fun getNoOpportunityCount(profitGroup: TwoLegArbitrageRelativeProfitGroup): Map<CurrencyPairWithExchangePair, Long> {
        synchronized(noOpportunityCount[profitGroup]!!) {
            return Collections.unmodifiableMap(noOpportunityCount[profitGroup]!!)
        }
    }

    fun clearNoOpportunityCount(profitGroup: TwoLegArbitrageRelativeProfitGroup) {
        synchronized(noOpportunityCount[profitGroup]!!) {
            return noOpportunityCount[profitGroup]!!.clear()
        }
    }

    fun getCurrencyPairWithExchangePairs(profitGroup: TwoLegArbitrageRelativeProfitGroup) = profitGroups[profitGroup]!!.keys.toList()

    fun getExchangePairsOpportunityCount(profitGroup: TwoLegArbitrageRelativeProfitGroup): List<ExchangePairWithOpportunityCount> {
        val profits = profitGroups[profitGroup]!!
        val result = HashMap<ExchangePair, Long>()
        synchronized(profits) {
            profits.forEach {
                if (!result.containsKey(it.key.exchangePair)) {
                    result[it.key.exchangePair] = 0
                }
                result[it.key.exchangePair] = result[it.key.exchangePair]!! + oneIfHasOpportunity(it.value)
            }
        }
        return result.map {
            ExchangePairWithOpportunityCount(
                exchangePair = it.key,
                opportunityCount = it.value
            )
        }
    }

    private fun oneIfHasOpportunity(profit: TwoLegOrderBookArbitrageProfit): Int {
        return if (profit.orderBookArbitrageProfitHistogram.find { it != null } != null) {
            1
        } else {
            0
        }
    }

    fun removeTooOldProfits() {
        val currentTimeMs = currentTimeMillisFunction()
        profitGroups.forEach { profitGroupEntry ->
            getCurrencyPairWithExchangePairs(profitGroupEntry.key).forEach {
                synchronized(profitGroupEntry.value) {
                    if (profitGroupEntry.value.containsKey(it)) {
                        if (currentTimeMs - profitGroupEntry.value[it]!!.calculatedAtMillis > ageOfOldestTwoLegArbitrageProfitToKeepMs) {
                            profitGroupEntry.value.remove(it)
                        }
                    }
                }

            }
        }
    }

}
