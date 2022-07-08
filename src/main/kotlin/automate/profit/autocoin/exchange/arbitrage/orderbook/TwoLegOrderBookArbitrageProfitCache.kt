package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import mu.KLogging
import java.util.concurrent.ConcurrentHashMap

class TwoLegOrderBookArbitrageProfitCache(
    private val ageOfOldestTwoLegArbitrageProfitToKeepMs: Long,
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis
) {
    private val profitGroups: Map<TwoLegArbitrageRelativeProfitGroup, ConcurrentHashMap<CurrencyPairWithExchangePair, TwoLegOrderBookArbitrageProfit>> =
        TwoLegArbitrageRelativeProfitGroup
            .values()
            .associateWith { ConcurrentHashMap<CurrencyPairWithExchangePair, TwoLegOrderBookArbitrageProfit>() }

    companion object : KLogging()

    fun setProfit(profitGroup: TwoLegArbitrageRelativeProfitGroup, profit: TwoLegOrderBookArbitrageProfit) {
        logger.debug { "Setting profit $profit" }
        synchronized(profitGroups[profitGroup]!!) {
            profitGroups[profitGroup]!![profit.currencyPairWithExchangePair] = profit
        }
    }

    fun removeProfit(profitGroup: TwoLegArbitrageRelativeProfitGroup, currencyPairWithExchangePair: CurrencyPairWithExchangePair) {

        synchronized(profitGroups[profitGroup]!!) {
            if (profitGroups[profitGroup]!!.contains(currencyPairWithExchangePair)) {
                logger.debug { "Removing profit for key $currencyPairWithExchangePair" }
            }
            profitGroups[profitGroup]!!.remove(currencyPairWithExchangePair)
        }
    }

    fun getProfit(profitGroup: TwoLegArbitrageRelativeProfitGroup, currencyPairWithExchangePair: CurrencyPairWithExchangePair): TwoLegOrderBookArbitrageProfit? {
        return profitGroups[profitGroup]!![currencyPairWithExchangePair]
    }

    fun getCurrencyPairWithExchangePairs(profitGroup: TwoLegArbitrageRelativeProfitGroup) = profitGroups[profitGroup]!!.keys.toList()

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