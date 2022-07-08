package automate.profit.autocoin.logger

import mu.KLogger
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

val periodicalLoggerScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()

class PeriodicalLogger(
    private val wrapped: KLogger,
    private val flushLogsInterval: Duration = Duration.of(1L, ChronoUnit.HOURS),
    private val loggerExecutorService: ScheduledExecutorService = periodicalLoggerScheduledExecutorService,
    private val currentTimeMillisFunction: () -> Long = { System.currentTimeMillis() }
) : KLogger by wrapped {
    private data class ThrowableWithMessage(
        val throwable: Throwable?,
        val message: String
    )

    private data class LogLevelContext(
        val lastOccurrenceMillisMap: ConcurrentHashMap<String, Long> = ConcurrentHashMap<String, Long>(),
        val firstOccurrenceMillisMap: ConcurrentHashMap<String, Long> = ConcurrentHashMap<String, Long>(),
        val occurrenceCountMap: ConcurrentHashMap<String, Long> = ConcurrentHashMap<String, Long>(),
        val occurrenceInstanceMap: ConcurrentHashMap<String, ThrowableWithMessage> = ConcurrentHashMap<String, ThrowableWithMessage>(),
    )

    private enum class LogLevel {
        INFO,
        ERROR,
    }

    private val lock = ReentrantLock()
    private val logLevelContextMap = LogLevel.values().associateWith { LogLevelContext() }
    private val nullMessage = "null"


    fun scheduleLogFlush(): PeriodicalLogger {
        loggerExecutorService.scheduleAtFixedRate({ logFlush() }, flushLogsInterval.toSeconds(), flushLogsInterval.toSeconds(), TimeUnit.SECONDS)
        return this
    }

    private fun logFlush() {
        lock.lock()
        try {
            logLevelContextMap.forEach { logLevelContext ->
                logLevelContext.value.occurrenceInstanceMap.forEach {
                    val howManyTimesOccurred = logLevelContext.value.occurrenceCountMap.getValue(it.key)
                    val firstOccurrenceMillis = logLevelContext.value.firstOccurrenceMillisMap.getValue(it.key)
                    val lastOccurrenceMillis = logLevelContext.value.lastOccurrenceMillisMap.getValue(it.key)
                    when (logLevelContext.key) {
                        LogLevel.INFO ->
                            wrapped.info(it.value.throwable) { "[Multiple info occurrence: $howManyTimesOccurred times between $firstOccurrenceMillis and $lastOccurrenceMillis ($flushLogsInterval) ]\n${it.value.message}" }
                        LogLevel.ERROR
                        -> wrapped.error(it.value.throwable) { "[Multiple error occurrence: $howManyTimesOccurred times between $firstOccurrenceMillis and $lastOccurrenceMillis ($flushLogsInterval) ]\n${it.value.message}" }
                    }
                }
                logLevelContext.value.occurrenceCountMap.clear()
                logLevelContext.value.occurrenceInstanceMap.clear()
            }
        } catch (e: Exception) {
            wrapped.error(e) { "Something went wrong during logFlush" }
        } finally {
            lock.unlock()
        }
    }

    /**
     * Aggregate logs by Throwable.message
     * Log only first occurrence until next flush.
     * On flush log time window and how many times error occurred
     *
     * T0
     * +logA - submit logging it
     * +logA - add to aggregated data
     * +logA - add to aggregated data
     * +logB - submit logging it
     * T1 - flush
     * log logA + count of occurrences during time window
     *
     */
    fun frequentError(t: Throwable, messageFunction: () -> String) {
        frequentLog(LogLevel.ERROR, t, messageFunction)
    }

    fun frequentInfo(messageFunction: () -> String) {
        frequentLog(LogLevel.INFO, null, messageFunction)
    }

    private fun frequentLog(logLevel: LogLevel, t: Throwable?, messageFunction: () -> String) {
        loggerExecutorService.submit {
            lock.lock()
            try {
                val message = messageFunction()
                val key = if (t == null) message else t.message ?: nullMessage
                val currentTimeMillis = currentTimeMillisFunction()

                val logLevelContext = logLevelContextMap[logLevel]!!
                if (!logLevelContext.occurrenceCountMap.containsKey(key)) {

                    val message = messageFunction()
                    wrapped.error(message, t)
                    logLevelContext.occurrenceCountMap[key] = 1L
                    logLevelContext.occurrenceInstanceMap[key] = ThrowableWithMessage(throwable = t, message = message)
                    logLevelContext.firstOccurrenceMillisMap[key] = currentTimeMillis
                } else {
                    logLevelContext.occurrenceCountMap[key] = logLevelContext.occurrenceCountMap[key]!! + 1
                }
                logLevelContext.lastOccurrenceMillisMap[key] = currentTimeMillis
            } finally {
                lock.unlock()
            }
        }

    }
}