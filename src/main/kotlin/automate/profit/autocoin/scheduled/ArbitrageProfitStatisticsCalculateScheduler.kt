package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCache
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCalculator
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class ArbitrageProfitStatisticsCalculateScheduler(
        private val twoLegArbitrageProfitStatisticsCalculator: TwoLegArbitrageProfitStatisticsCalculator,
        private val twoLegArbitrageProfitStatisticsCache: TwoLegArbitrageProfitStatisticsCache,
        private val executorService: ScheduledExecutorService
) {
    companion object : KLogging()

    fun scheduleCacheRefresh() {
        logger.info { "Scheduling refresh profit statistics cache every 5 minutes" }
        executorService.scheduleAtFixedRate({
            try {
                val newStatistics = twoLegArbitrageProfitStatisticsCalculator.calculateAllStatistics()
                twoLegArbitrageProfitStatisticsCache.twoLegArbitrageProfitStatistics.set(newStatistics)

            } catch (e: Exception) {
                logger.error(e) { "Could not calculate profit statistics" }
            }
        }, 0, 5, TimeUnit.MINUTES)
    }
}