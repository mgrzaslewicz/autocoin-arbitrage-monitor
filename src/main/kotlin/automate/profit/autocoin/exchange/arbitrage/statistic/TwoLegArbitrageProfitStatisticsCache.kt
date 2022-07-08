package automate.profit.autocoin.exchange.arbitrage.statistic

import java.util.concurrent.atomic.AtomicReference

class TwoLegArbitrageProfitStatisticsCache {
    val twoLegArbitrageProfitStatistics: AtomicReference<List<TwoLegArbitrageProfitStatistic>> = AtomicReference(emptyList())
}
