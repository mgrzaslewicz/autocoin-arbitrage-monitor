package automate.profit.autocoin.exchange.arbitrage.ticker

import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import mu.KLogging
import java.util.concurrent.ConcurrentHashMap

class TwoLegTickerArbitrageProfitCache(
        private val ageOfOldestTwoLegArbitrageProfitToKeepMs: Long,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {
    private val profits = ConcurrentHashMap<CurrencyPairWithExchangePair, TwoLegTickerArbitrageProfit>()

    companion object : KLogging()

    fun addProfit(profit: TwoLegTickerArbitrageProfit) {
        logger.debug { "Setting profit $profit" }
        synchronized(profits) {
            profits[profit.currencyPairWithExchangePair] = profit
        }
    }

    fun removeProfit(currencyPairWithExchangePair: CurrencyPairWithExchangePair) {
        synchronized(profits) {
            if (profits.contains(currencyPairWithExchangePair)) {
                logger.debug { "Removing profit for key $currencyPairWithExchangePair" }
            }
            profits.remove(currencyPairWithExchangePair)
        }
    }

    fun getProfit(currencyPairWithExchangePair: CurrencyPairWithExchangePair): TwoLegTickerArbitrageProfit {
        return profits.getValue(currencyPairWithExchangePair)
    }

    fun getCurrencyPairWithExchangePairs() = profits.keys.toList()

    fun removeTooOldProfits() {
        val currentTimeMs = currentTimeMillis()
        getCurrencyPairWithExchangePairs().forEach {
            synchronized(profits) {
                if (profits.containsKey(it)) {
                    if (currentTimeMs - profits[it]!!.calculatedAtMillis > ageOfOldestTwoLegArbitrageProfitToKeepMs) {
                        profits.remove(it)
                    }
                }
            }
        }
    }

}