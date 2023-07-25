package automate.profit.autocoin.scheduled

import automate.profit.autocoin.health.HealthService
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.ExchangePairWithOpportunityCount
import automate.profit.autocoin.metrics.MetricsService
import mu.KLogging
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class HealthMetricsScheduler(
    private val interval: Duration = Duration.of(1, ChronoUnit.MINUTES),
    private val healthService: HealthService,
    private val metricsService: MetricsService,
    private val executorService: ScheduledExecutorService
) {
    companion object : KLogging()

    private fun reportMemoryUsage() {
        metricsService.recordMemory()
    }

    private fun reportHealth(healthy: Boolean) {
        metricsService.recordHealth(healthy)
    }

    private fun reportDescriptorsUsage() {
        metricsService.recordDescriptors()
    }

    private fun reportThreadsUsage() {
        metricsService.recordThreadCount()
    }

    private fun recordExchangePairOpportunityCounts(exchangePairsOpportunityCount: List<ExchangePairWithOpportunityCount>) {
        exchangePairsOpportunityCount.forEach {
            metricsService.recordExchangePairOpportunityCount(it)
        }
    }

    private fun recordExchangeReceivedOrderBooksSinceStart(receivedOrderBooksSinceStart: Map<SupportedExchange, Long>) {
        receivedOrderBooksSinceStart.forEach {
            metricsService.recordReceivedOrderBooksSinceStart(it.key.exchangeName, it.value)
        }
    }

    private fun recordExchangeReceivedTickersSinceStart(receivedTickersSinceStart: Map<SupportedExchange, Long>) {
        receivedTickersSinceStart.forEach {
            metricsService.recordReceivedTickersSinceStart(it.key.exchangeName, it.value)
        }
    }

    fun scheduleSendingMetrics() {
        logger.info { "Scheduling sending metrics every ${interval}: health, memory usage, threads count, open files count, exchange opportunity counts" }
        executorService.scheduleAtFixedRate({
            try {
                val health = healthService.getHealth()
                reportHealth(health.healthy)
                reportMemoryUsage()
                reportThreadsUsage()
                reportDescriptorsUsage()
                recordExchangePairOpportunityCounts(health.twoLegArbitrageOpportunities.exchangePairsWithOpportunityCount)
                recordExchangeReceivedTickersSinceStart(health.receivedTickersSinceStart)
                recordExchangeReceivedOrderBooksSinceStart(health.receivedOrderBooksSinceStart)
            } catch (e: Exception) {
                logger.error(e) { "Something went wrong when sending metrics" }
            }
        }, 0, interval.seconds, TimeUnit.SECONDS)

    }

}
