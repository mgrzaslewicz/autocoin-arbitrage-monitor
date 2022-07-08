package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCache
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TwoLegOrderBookArbitrageProfitCacheScheduler(
    private val scheduledExecutorService: ScheduledExecutorService,
    private val ageOfOldestTwoLegArbitrageProfitToKeepMs: Long,
    private val twoLegArbitrageProfitOpportunityCache: TwoLegArbitrageProfitOpportunityCache,
) {
    private companion object : KLogging()

    fun scheduleRemovingTooOldAndSendingMetrics() {
        logger.info { "Scheduling removing too old profits every ${ageOfOldestTwoLegArbitrageProfitToKeepMs}ms" }
        scheduledExecutorService.scheduleAtFixedRate({
            removeTooOldMetrics()
        }, ageOfOldestTwoLegArbitrageProfitToKeepMs + 1000, ageOfOldestTwoLegArbitrageProfitToKeepMs, TimeUnit.MILLISECONDS)
    }

    private fun removeTooOldMetrics() {
        try {
            twoLegArbitrageProfitOpportunityCache.removeTooOldProfits()
        } catch (e: Exception) {
            logger.error(e) { "Could not remove too old profits" }
        }
    }

}
