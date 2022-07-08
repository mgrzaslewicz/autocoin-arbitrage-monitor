package automate.profit.autocoin.scheduled

import autocoin.metrics.MetricsService
import automate.profit.autocoin.exchange.orderbookstream.OrderBookSseStreamService
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class HealthMetricsScheduler(
    private val orderBookSseStreamService: OrderBookSseStreamService,
    private val metricsService: MetricsService,
    private val executorService: ScheduledExecutorService
) {
    companion object : KLogging()

    fun reportMemoryUsage() {
        metricsService.recordMemory()
    }

    fun reportHealth() {
        metricsService.recordHealth(orderBookSseStreamService.isConnected())
    }

    fun reportDescriptorsUsage() {
        metricsService.recordDescriptors()
    }

    fun reportThreadsUsage() {
        metricsService.recordThreadCount()
    }

    fun scheduleSendingMetrics() {
        val interval = 60L
        logger.info { "Scheduling sending metrics every ${interval}s: health, memory usage, threads count, open files count" }
        executorService.scheduleAtFixedRate({
            try {
                reportHealth()
                reportMemoryUsage()
                reportThreadsUsage()
                reportDescriptorsUsage()
            } catch (e: Exception) {
                logger.error(e) { "Something went wrong when sending metrics" }
            }
        }, 0, interval, TimeUnit.SECONDS)

    }
}
