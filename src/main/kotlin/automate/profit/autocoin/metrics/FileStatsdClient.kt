package automate.profit.autocoin.metrics

import com.timgroup.statsd.Event
import com.timgroup.statsd.ServiceCheck
import com.timgroup.statsd.StatsDClient
import mu.KLogging
import java.io.File
import java.nio.file.Path
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors

class FileStatsdClient(private val metricsFolder: String, private val currentTimeMillis: () -> Long = System::currentTimeMillis) : StatsDClient {
    private companion object : KLogging()

    private val executor = Executors.newSingleThreadExecutor()
    private val recordedTimeBuffer = ConcurrentHashMap<String, MutableList<Long>>()

    override fun recordDistributionValue(aspect: String, value: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordDistributionValue(aspect: String, value: Double, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordDistributionValue(aspect: String, value: Long, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordDistributionValue(aspect: String, value: Long, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordGaugeValue(aspect: String, value: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordGaugeValue(aspect: String, value: Double, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordGaugeValue(aspect: String, value: Long, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordGaugeValue(aspect: String, value: Long, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun decrementCounter(aspect: String, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun decrementCounter(aspect: String, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    fun saveMetricsToFileAndClearBuffer() {
        recordedTimeBuffer.forEach {
            synchronized(it.value) {
                var sum = 0L
                var min = Long.MAX_VALUE
                var max = Long.MIN_VALUE
                val timeList = it.value
                timeList.forEach { timeMs ->
                    max = Math.max(timeMs, max)
                    min = Math.min(timeMs, min)
                    sum += timeMs
                }
                val avg = sum.toDouble() / it.value.size
                val metricsFolderDirectory = File(metricsFolder)
                metricsFolderDirectory.mkdirs()
                val aspectFileNamePlusTags = it.key.split("!@#")
                val tags = if(aspectFileNamePlusTags.size == 2) {
                    aspectFileNamePlusTags[1]
                } else ""
                val aspectFile = metricsFolderDirectory.resolve(aspectFileNamePlusTags[0])
                try {
                    aspectFile.appendText("${currentTimeMillis()},$min,$max,$avg,$tags\n")
                } catch (e: Exception) {
                    logger.error(e) { "Could not save metrics ${it.key}" }
                } finally { // whether save was successful or not, clear buffer. metrics are not crucial
                    timeList.clear()
                }
            }
        }
    }

    override fun recordExecutionTime(aspect: String, timeInMs: Long, vararg tags: String) {
        executor.submit {
            val key = aspect + "!@#" + tags.joinToString(":")
            recordedTimeBuffer.computeIfAbsent(key) { ArrayList() }
            val list = recordedTimeBuffer[key]!!
            synchronized(list) {
                recordedTimeBuffer[key]!!.add(timeInMs)
            }
        }
    }

    override fun recordExecutionTime(aspect: String, timeInMs: Long, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun time(aspect: String, value: Long, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun time(aspect: String, value: Long, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun histogram(aspect: String, value: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun histogram(aspect: String, value: Double, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun histogram(aspect: String, value: Long, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun histogram(aspect: String, value: Long, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun serviceCheck(sc: ServiceCheck?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun count(aspect: String, delta: Long, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun count(aspect: String, delta: Long, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun count(aspect: String, delta: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun count(aspect: String, delta: Double, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordSetValue(aspect: String, value: String, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun stop() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordHistogramValue(aspect: String, value: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordHistogramValue(aspect: String, value: Double, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordHistogramValue(aspect: String, value: Long, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordHistogramValue(aspect: String, value: Long, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordServiceCheckRun(sc: ServiceCheck?) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun incrementCounter(aspect: String, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun incrementCounter(aspect: String, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun gauge(aspect: String, value: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun gauge(aspect: String, value: Double, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun gauge(aspect: String, value: Long, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun gauge(aspect: String, value: Long, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun increment(aspect: String, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun increment(aspect: String, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun close() {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun distribution(aspect: String, value: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun distribution(aspect: String, value: Double, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun distribution(aspect: String, value: Long, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun distribution(aspect: String, value: Long, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun recordEvent(event: Event?, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun decrement(aspect: String, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun decrement(aspect: String, sampleRate: Double, vararg tags: String) {
        TODO("not implemented") //To change body of created functions use File | Settings | File Templates.
    }
}