package automate.profit.autocoin.config

import automate.profit.autocoin.exchange.TickerFetchScheduler
import automate.profit.autocoin.exchange.ticker.TickerListener
import automate.profit.autocoin.exchange.ticker.TickerListenerRegistrars
import mu.KLogging

class AppStarter(
        private val tickerListeners: List<TickerListener>,
        private val tickerFetchScheduler: TickerFetchScheduler,
        private val tickerListenerRegistrars: TickerListenerRegistrars
) {
    companion object : KLogging()

    fun start() {
        logger.info { "Registering ${tickerListeners.size} ticker listeners" }
        tickerListeners.forEach { tickerListenerRegistrars.registerTickerListener(it) }
        logger.info { "Scheduling getting tickers" }
        tickerFetchScheduler.scheduleFetchingTickers()
    }
}