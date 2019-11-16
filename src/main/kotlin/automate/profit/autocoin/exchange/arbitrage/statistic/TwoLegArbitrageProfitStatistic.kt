package automate.profit.autocoin.exchange.arbitrage.statistic

import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import java.math.BigDecimal

data class ProfitOpportunityCount(
        val relativeProfitThreshold: BigDecimal,
        val count: Int
)


data class TwoLegArbitrageProfitStatistic(
        val currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        val average: BigDecimal,
        val min: BigDecimal,
        val max: BigDecimal,
        val profitOpportunityHistogram: List<ProfitOpportunityCount>
)
