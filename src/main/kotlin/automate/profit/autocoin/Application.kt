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
                commonExchangeCurrencyPairsService = appContext.commonExchangeCurrencyPairsService,
                tickerListenersProvider = appContext.tickerListenersProvider,
                tickerFetchScheduler = appContext.tickerFetchScheduler,
                tickerListenerRegistrars = appContext.tickerListenerRegistrars,
                tickerPairsSaveScheduler = appContext.tickerPairsSaveScheduler,
                profitStatisticsCalculateScheduler = appContext.arbitrageProfitStatisticCalculateScheduler,
                server = appContext.server
        )
        appStarter.start()
    }
    logger.info { "Started in $bootTimeMillis ms" }
}
