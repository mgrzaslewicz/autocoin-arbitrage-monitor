package automate.profit.autocoin.config

import automate.profit.autocoin.scheduled.TickerFetchScheduler
import automate.profit.autocoin.exchange.ticker.TickerListener
import automate.profit.autocoin.exchange.ticker.TickerListenerRegistrars
import automate.profit.autocoin.scheduled.TickerPairsSaveScheduler
import mu.KLogging

class AppStarter(
        private val tickerListeners: List<TickerListener>,
        private val tickerFetchScheduler: TickerFetchScheduler,
        private val tickerListenerRegistrars: TickerListenerRegistrars,
        private val tickerPairsSaveScheduler: TickerPairsSaveScheduler
) {
    companion object : KLogging()

    fun start() {
        logger.info { "Registering ${tickerListeners.size} ticker listeners" }
        tickerListeners.forEach { tickerListenerRegistrars.registerTickerListener(it) }
        logger.info { "Scheduling getting tickers" }
        tickerFetchScheduler.scheduleFetchingTickers()
        logger.info { "Scheduling saving tickers to files" }
        tickerPairsSaveScheduler.scheduleSavingTickerPairs()
    }
}