package automate.profit.autocoin.exchange.arbitrage.statistic

import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import java.math.BigDecimal

data class ProfitOpportunityCount(
        val relativeProfitThreshold: BigDecimal,
        val count: Int
)

data class ProfitStatisticHistogramByUsdDepth(
        val usdDepthTo: BigDecimal,
        val profitOpportunityHistogram: List<ProfitOpportunityCount>,
        val average: BigDecimal,
        val min: BigDecimal,
        val max: BigDecimal
)

data class TwoLegArbitrageProfitStatistic(
        val currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        val minUsd24hVolume: BigDecimal,
        val profitStatisticHistogramByUsdDepth: List<ProfitStatisticHistogramByUsdDepth>
)
