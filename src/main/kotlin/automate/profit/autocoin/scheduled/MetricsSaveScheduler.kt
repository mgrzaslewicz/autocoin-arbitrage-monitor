package automate.profit.autocoin.scheduled

import automate.profit.autocoin.metrics.FileStatsdClient
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class MetricsSaveScheduler(
        private val fileStatsdClient: FileStatsdClient,
        private val executorService: ScheduledExecutorService
) {
    companion object : KLogging()

    fun scheduleSavingMetrics() {
        logger.info { "Will save metrics every 1 minute" }
        executorService.scheduleAtFixedRate({
            try {
                fileStatsdClient.saveMetricsToFileAndClearBuffer()
            } catch (e: Exception) {
                logger.error(e) { "Could not save metrics" }
            }
        }, 0, 60, TimeUnit.SECONDS)
    }
}