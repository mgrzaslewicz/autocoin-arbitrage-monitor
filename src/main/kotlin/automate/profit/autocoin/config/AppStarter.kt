package automate.profit.autocoin.config

import automate.profit.autocoin.exchange.ticker.TickerListener
import automate.profit.autocoin.exchange.ticker.TickerListenerRegistrars
import automate.profit.autocoin.exchange.ticker.TickerPairCacheLoader
import automate.profit.autocoin.scheduled.TickerFetchScheduler
import automate.profit.autocoin.scheduled.TickerPairsSaveScheduler
import mu.KLogging

class AppStarter(
        private val tickerListeners: List<TickerListener>,
        private val tickerFetchScheduler: TickerFetchScheduler,
        private val tickerListenerRegistrars: TickerListenerRegistrars,
        private val tickerPairsSaveScheduler: TickerPairsSaveScheduler,
        private val tickerPairCacheLoader: TickerPairCacheLoader
) {
    companion object : KLogging()

    fun start() {
        logger.info { "Loading all previously saved ticker pairs to cache" }
        tickerPairCacheLoader.loadAllSavedTickerPairs()
        logger.info { "Registering ${tickerListeners.size} ticker listeners" }
        tickerListeners.forEach { tickerListenerRegistrars.registerTickerListener(it) }
        logger.info { "Scheduling fetching tickers" }
        tickerFetchScheduler.scheduleFetchingTickers()
        logger.info { "Scheduling saving tickers to files" }
        tickerPairsSaveScheduler.scheduleSavingTickerPairs()
    }
}