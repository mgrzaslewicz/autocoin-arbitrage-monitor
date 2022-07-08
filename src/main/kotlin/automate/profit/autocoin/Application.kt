package automate.profit.autocoin

import automate.profit.autocoin.config.AppContext
import automate.profit.autocoin.config.loadConfig
import mu.KotlinLogging
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val bootTimeMillis = measureTimeMillis {
        val config = loadConfig()
        logger.info { "Config: $config" }
        val appContext = AppContext(config)
        appContext.start()
    }
    logger.info { "Started in $bootTimeMillis ms" }
}
