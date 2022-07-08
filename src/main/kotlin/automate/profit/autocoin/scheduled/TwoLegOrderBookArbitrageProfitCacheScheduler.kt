package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageRelativeProfitGroup
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import automate.profit.autocoin.metrics.MetricsService
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TwoLegOrderBookArbitrageProfitCacheScheduler(
    private val scheduledExecutorService: ScheduledExecutorService,
    private val ageOfOldestTwoLegArbitrageProfitToKeepMs: Long,
    private val twoLegOrderBookArbitrageProfitCache: TwoLegOrderBookArbitrageProfitCache,
    private val metricsService: MetricsService,
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
            TwoLegArbitrageRelativeProfitGroup.values().forEach { profitGroup ->
                val exchangeOpportunityCount = SupportedExchange.values().associateWith { 0L }.toMutableMap()

                val exchangePairsOpportunityCount = twoLegOrderBookArbitrageProfitCache.getExchangePairsOpportunityCount(profitGroup)
                exchangePairsOpportunityCount.forEach {
                    metricsService.recordExchangePairOpportunityCount(profitGroup, it)
                    exchangeOpportunityCount[it.exchangePair.firstExchange] = exchangeOpportunityCount[it.exchangePair.firstExchange]!! + 1
                    exchangeOpportunityCount[it.exchangePair.secondExchange] = exchangeOpportunityCount[it.exchangePair.secondExchange]!! + 1
                }
                exchangeOpportunityCount.forEach {
                    metricsService.recordExchangeOpportunityCount(profitGroup, it.key, it.value)
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Could not send metrics" }
        }
    }

}
