package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TwoLegOrderBookArbitrageProfitCacheScheduler(
    private val scheduledExecutorService: ScheduledExecutorService,
    private val ageOfOldestTwoLegArbitrageProfitToKeepMs: Long,
    private val twoLegOrderBookArbitrageProfitCache: TwoLegOrderBookArbitrageProfitCache
) {
    private companion object : KLogging()

    fun scheduleRemovingTooOldAndSendingMetrics() {
        logger.info { "Scheduling removing too old profits and sending metrics every ${ageOfOldestTwoLegArbitrageProfitToKeepMs}ms" }
        scheduledExecutorService.scheduleAtFixedRate({
            removeTooMetricsOldAndSendMetrics()
            sendMetrics()
        }, ageOfOldestTwoLegArbitrageProfitToKeepMs + 1000, ageOfOldestTwoLegArbitrageProfitToKeepMs, TimeUnit.MILLISECONDS)
    }

    private fun removeTooMetricsOldAndSendMetrics() {
        try {
            twoLegOrderBookArbitrageProfitCache.removeTooOldProfits()
        } catch (e: Exception) {
            logger.error(e) { "Could not remove too old profits" }
        }
        // TODO add sending metrics to count how many opportunities there are between exchange pairs
    }

    private fun sendMetrics() {
        try {
            // TODO add sending metrics to count how many opportunities there are between exchange pairs
        } catch (e: Exception) {
            logger.error(e) { "Could not send metrics" }
        }
    }
}