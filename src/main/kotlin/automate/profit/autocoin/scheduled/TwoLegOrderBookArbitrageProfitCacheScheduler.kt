package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCache
import mu.KLogging
import java.time.Duration
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TwoLegOrderBookArbitrageProfitCacheScheduler(
    private val scheduledExecutorService: ScheduledExecutorService,
    private val ageOfOldestTwoLegArbitrageProfitToKeep: Duration,
    private val twoLegArbitrageProfitOpportunityCache: TwoLegArbitrageProfitOpportunityCache,
) {
    private companion object : KLogging()

    fun scheduleRemovingTooOldAndSendingMetrics() {
        logger.info { "Scheduling removing too old profits every $ageOfOldestTwoLegArbitrageProfitToKeep" }
        scheduledExecutorService.scheduleAtFixedRate(
            {
                removeTooOldMetrics()
            },
            ageOfOldestTwoLegArbitrageProfitToKeep.toMillis() + 1000,
            ageOfOldestTwoLegArbitrageProfitToKeep.toMillis(),
            TimeUnit.MILLISECONDS
        )
    }

    private fun removeTooOldMetrics() {
        try {
            twoLegArbitrageProfitOpportunityCache.removeTooOldProfits()
        } catch (e: Exception) {
            logger.error(e) { "Could not remove too old profits" }
        }
    }

}
