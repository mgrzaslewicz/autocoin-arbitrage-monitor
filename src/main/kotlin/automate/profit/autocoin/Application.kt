package automate.profit.autocoin

import automate.profit.autocoin.config.AppContext
import automate.profit.autocoin.config.AppStarter
import automate.profit.autocoin.config.loadConfig
import mu.KotlinLogging
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger { }

fun main(args: Array<String>) {
    val bootTimeMillis = measureTimeMillis {
        val config = loadConfig()
        logger.info { "Config: $config" }
        val appContext = AppContext(config)
        val appStarter = AppStarter(
                tickerListeners = appContext.tickerListeners,
                tickerFetchScheduler = appContext.tickerFetchScheduler,
                tickerListenerRegistrars = appContext.tickerListenerRegistrars,
                tickerPairsSaveScheduler = appContext.tickerPairsSaveScheduler,
                tickerPairCacheLoader = appContext.tickerPairCacheLoader,
                server = appContext.server
        )
        appStarter.start()
    }
    logger.info { "Started in $bootTimeMillis ms" }
}
