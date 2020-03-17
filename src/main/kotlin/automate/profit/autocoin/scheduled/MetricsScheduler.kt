package automate.profit.autocoin.scheduled

import autocoin.metrics.MetricsService
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MetricsScheduler(
        private val metricsService: MetricsService,
        private val executorService: ScheduledExecutorService
) {
    companion object : KLogging()

    fun reportMemoryUsage() {
        val interval = 60L
        logger.info { "Will record memory usage metrics every $interval seconds" }
        executorService.scheduleAtFixedRate({ metricsService.recordMemory() }, 0, interval, TimeUnit.SECONDS)
    }

    fun reportHealth() {
        val interval = 60L
        logger.info { "Will record health status metrics every $interval seconds" }
        executorService.scheduleAtFixedRate({ metricsService.recordHealth(true) }, 0, interval, TimeUnit.SECONDS)
    }

    fun reportDescriptorsUsage() {
        val interval = 60L
        logger.info { "Will record descriptor usage metrics every $interval seconds" }
        executorService.scheduleAtFixedRate({ metricsService.recordDescriptors() }, 0, interval, TimeUnit.SECONDS)
    }
}