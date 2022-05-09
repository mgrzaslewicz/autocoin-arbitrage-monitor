package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.ExchangePairWithOpportunityCount
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCache
import automate.profit.autocoin.metrics.MetricsService
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TwoLegOrderBookArbitrageProfitCacheScheduler(
    private val scheduledExecutorService: ScheduledExecutorService,
    private val ageOfOldestTwoLegArbitrageProfitToKeepMs: Long,
    private val twoLegArbitrageProfitOpportunityCache: TwoLegArbitrageProfitOpportunityCache,
    private val metricsService: MetricsService,
) {
    private companion object : KLogging()

    fun scheduleRemovingTooOldAndSendingMetrics() {
        logger.info { "Scheduling removing too old profits and sending metrics every ${ageOfOldestTwoLegArbitrageProfitToKeepMs}ms" }
        scheduledExecutorService.scheduleAtFixedRate({
            removeTooOldMetrics()
            sendMetrics()
        }, ageOfOldestTwoLegArbitrageProfitToKeepMs + 1000, ageOfOldestTwoLegArbitrageProfitToKeepMs, TimeUnit.MILLISECONDS)
    }

    private fun removeTooOldMetrics() {
        try {
            twoLegArbitrageProfitOpportunityCache.removeTooOldProfits()
        } catch (e: Exception) {
            logger.error(e) { "Could not remove too old profits" }
        }
    }

    private fun sendMetrics() {
        try {
            val exchangePairsOpportunityCount = twoLegArbitrageProfitOpportunityCache.getExchangePairsOpportunityCount()
            sendExchangePairOpportunityCounts(exchangePairsOpportunityCount)
            sendExchangeOpportunityCounts(exchangePairsOpportunityCount)
            sendExchangeNoOpportunityFoundCounts()
        } catch (e: Exception) {
            logger.error(e) { "Could not send metrics" }
        }
    }

    private fun sendExchangeNoOpportunityFoundCounts() {
        val noOpportunityCount = twoLegArbitrageProfitOpportunityCache.getNoOpportunityCount()
        val exchangeNoOpportunityCount = SupportedExchange.values().associateWith { 0L }.toMutableMap()
        noOpportunityCount.forEach {
            exchangeNoOpportunityCount[it.key.exchangePair.firstExchange] = exchangeNoOpportunityCount[it.key.exchangePair.firstExchange]!! + it.value
            exchangeNoOpportunityCount[it.key.exchangePair.secondExchange] = exchangeNoOpportunityCount[it.key.exchangePair.secondExchange]!! + it.value
        }
        exchangeNoOpportunityCount.forEach {
            metricsService.recordExchangeNoOpportunityFoundCount(it.key, it.value)
            twoLegArbitrageProfitOpportunityCache.clearNoOpportunityCount()
        }
    }

    private fun sendExchangeOpportunityCounts(exchangePairsOpportunityCount: List<ExchangePairWithOpportunityCount>) {
        val exchangeOpportunityCount = SupportedExchange.values().associateWith { 0L }.toMutableMap()
        exchangePairsOpportunityCount.forEach {
            exchangeOpportunityCount[it.exchangePair.firstExchange] = exchangeOpportunityCount[it.exchangePair.firstExchange]!! + 1
            exchangeOpportunityCount[it.exchangePair.secondExchange] = exchangeOpportunityCount[it.exchangePair.secondExchange]!! + 1
        }
        exchangeOpportunityCount.filter { it.value > 0 }.forEach {
            metricsService.recordExchangeOpportunityCount(it.key, it.value)
        }
    }

    private fun sendExchangePairOpportunityCounts(exchangePairsOpportunityCount: List<ExchangePairWithOpportunityCount>) {
        exchangePairsOpportunityCount.forEach {
            metricsService.recordExchangePairOpportunityCount(it)
        }
    }

}
