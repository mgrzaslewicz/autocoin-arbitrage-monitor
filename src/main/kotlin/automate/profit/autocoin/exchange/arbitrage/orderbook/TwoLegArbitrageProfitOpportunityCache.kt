package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.app.config.ExchangePair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import mu.KLogging
import java.time.Duration
import java.util.*
import java.util.concurrent.ConcurrentHashMap

// TODO change it to ExchangePairWithOpportunityStatistics
data class ExchangePairWithOpportunityCount(
    val exchangePair: ExchangePair,
    val opportunityCount: Long,
)

class TwoLegArbitrageProfitOpportunityCache(
    private val ageOfOldestTwoLegArbitrageProfitToKeep: Duration,
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
) {
    private val profits = ConcurrentHashMap<CurrencyPairWithExchangePair, TwoLegArbitrageProfitOpportunity>()
    private val noOpportunityCount: MutableMap<CurrencyPairWithExchangePair, Long> =
        ConcurrentHashMap<CurrencyPairWithExchangePair, Long>()

    companion object : KLogging()

    fun setProfitOpportunity(profit: TwoLegArbitrageProfitOpportunity) {
        logger.debug { "Setting profit $profit" }
        synchronized(profits) {
            profits[profit.currencyPairWithExchangePair] = profit
        }
    }

    fun removeProfitOpportunity(currencyPairWithExchangePair: CurrencyPairWithExchangePair) {
        synchronized(profits) {
            if (logger.isDebugEnabled) {
                if (profits.contains(currencyPairWithExchangePair)) {
                    logger.debug("Removing profit for key $currencyPairWithExchangePair")
                }
            }
            profits.remove(currencyPairWithExchangePair)
        }
        countNoOpportunityFound(currencyPairWithExchangePair)
    }

    private fun countNoOpportunityFound(currencyPairWithExchangePair: CurrencyPairWithExchangePair) {
        synchronized(noOpportunityCount) {
            if (!noOpportunityCount.containsKey(currencyPairWithExchangePair)) {
                noOpportunityCount[currencyPairWithExchangePair] = 0L
            }
            noOpportunityCount[currencyPairWithExchangePair] = noOpportunityCount[currencyPairWithExchangePair]!! + 1
        }
    }

    fun getProfit(currencyPairWithExchangePair: CurrencyPairWithExchangePair): TwoLegArbitrageProfitOpportunity? {
        return profits[currencyPairWithExchangePair]
    }

    fun getAllProfits(): Sequence<TwoLegArbitrageProfitOpportunity> {
        return getCurrencyPairWithExchangePairs()
            .asSequence()
            .mapNotNull { currencyPairWithExchangePair ->
                getProfit(currencyPairWithExchangePair)
            }
    }

    fun getNoOpportunityCount(): Map<CurrencyPairWithExchangePair, Long> {
        synchronized(noOpportunityCount) {
            return Collections.unmodifiableMap(noOpportunityCount)
        }
    }

    fun clearNoOpportunityCount() {
        synchronized(noOpportunityCount) {
            return noOpportunityCount.clear()
        }
    }

    fun getCurrencyPairWithExchangePairs() = profits.keys.toList()

    fun getExchangePairsOpportunityCount(): List<ExchangePairWithOpportunityCount> {
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

    private fun oneIfHasOpportunity(profit: TwoLegArbitrageProfitOpportunity): Int {
        return if (profit.profitOpportunityHistogram.find { it != null } != null) {
            1
        } else {
            0
        }
    }

    fun removeTooOldProfits() {
        val currentTimeMs = currentTimeMillisFunction()
        getCurrencyPairWithExchangePairs().forEach {
            synchronized(profits) {
                if (profits.containsKey(it)) {
                    if (currentTimeMs - profits[it]!!.calculatedAtMillis > ageOfOldestTwoLegArbitrageProfitToKeep.toMillis()) {
                        profits.remove(it)
                    }
                }
            }
        }
    }

}
