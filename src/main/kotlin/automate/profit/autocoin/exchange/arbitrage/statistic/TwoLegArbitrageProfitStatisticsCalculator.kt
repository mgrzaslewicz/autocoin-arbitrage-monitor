package automate.profit.autocoin.exchange.arbitrage.statistic

import automate.profit.autocoin.exchange.arbitrage.ticker.TwoLegTickerArbitrageProfitCalculator
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.FileTickerPairRepository
import java.math.BigDecimal
import java.math.RoundingMode.HALF_DOWN

class TwoLegArbitrageProfitStatisticsCalculator(private val fileTickerPairRepository: FileTickerPairRepository, private val twoLegTickerArbitrageProfitCalculator: TwoLegTickerArbitrageProfitCalculator) {

    private val zeroPoint5Percent = BigDecimal.valueOf(0.005)
    private val zeroPoint75Percent = BigDecimal.valueOf(0.0075)
    private val onePercent = BigDecimal.valueOf(0.01)
    private val onePoint5Percent = BigDecimal.valueOf(0.015)
    private val twoPercent = BigDecimal.valueOf(0.02)
    private val fivePercent = BigDecimal.valueOf(0.05)

    private fun BigDecimal.divide(by: Int): BigDecimal {
        if (this == BigDecimal.ZERO) {
            return BigDecimal.ZERO
        }
        return try {
            this.divide(by.toBigDecimal())
        } catch (e: ArithmeticException) {
            this.divide(by.toBigDecimal(), HALF_DOWN)
        }
    }

    fun calculateAllStatistics(): List<TwoLegArbitrageProfitStatistic> {
        return fileTickerPairRepository.getAllCurrencyPairsWithExchangePairs().mapNotNull {
            calculateStatistic(it)
        }
    }

    fun calculateStatistic(currencyPairWithExchangePair: CurrencyPairWithExchangePair): TwoLegArbitrageProfitStatistic? {
        val profits = fileTickerPairRepository.getTickerPairs(currencyPairWithExchangePair).mapNotNull { twoLegTickerArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, it) }
        return if (profits.isEmpty()) {
            null
        } else {
            var sum = BigDecimal.ZERO
            var min = BigDecimal.valueOf(Double.MAX_VALUE)
            var max = BigDecimal.valueOf(Double.MIN_VALUE)
            var howManyTimesProfitAbove0Point5Percent = 0
            var howManyTimesProfitAbove0Point75Percent = 0
            var howManyTimesProfitAbove1Percent = 0
            var howManyTimesProfitAbove1Point5Percent = 0
            var howManyTimesProfitAbove2Percent = 0
            var howManyTimesProfitAbove5Percent = 0

            profits.forEach {
                sum = sum.plus(it.relativeProfit)
                min = min.min(it.relativeProfit)
                max = max.max(it.relativeProfit)
                when {
                    it.relativeProfit > fivePercent -> howManyTimesProfitAbove5Percent++
                    it.relativeProfit > twoPercent -> howManyTimesProfitAbove2Percent++
                    it.relativeProfit > onePoint5Percent -> howManyTimesProfitAbove1Point5Percent++
                    it.relativeProfit > onePercent -> howManyTimesProfitAbove1Percent++
                    it.relativeProfit > zeroPoint75Percent -> howManyTimesProfitAbove0Point75Percent++
                    it.relativeProfit > zeroPoint5Percent -> howManyTimesProfitAbove0Point5Percent++
                }
            }

            val average = sum.divide(profits.size)
            return TwoLegArbitrageProfitStatistic(
                    currencyPairWithExchangePair = currencyPairWithExchangePair,
                    average = average,
                    min = min,
                    max = max,
                    minUsd24hVolume = profits.last().minUsd24hVolumeOfBothExchanges,
                    profitOpportunityHistogram = listOf(
                            ProfitOpportunityCount(zeroPoint5Percent, howManyTimesProfitAbove0Point5Percent),
                            ProfitOpportunityCount(zeroPoint75Percent, howManyTimesProfitAbove0Point75Percent),
                            ProfitOpportunityCount(onePercent, howManyTimesProfitAbove1Percent),
                            ProfitOpportunityCount(onePoint5Percent, howManyTimesProfitAbove1Point5Percent),
                            ProfitOpportunityCount(twoPercent, howManyTimesProfitAbove2Percent),
                            ProfitOpportunityCount(fivePercent, howManyTimesProfitAbove5Percent)
                    )
            )
        }
    }

}