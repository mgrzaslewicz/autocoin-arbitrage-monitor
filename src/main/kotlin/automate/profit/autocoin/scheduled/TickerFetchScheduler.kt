package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitCache
import automate.profit.autocoin.exchange.ticker.TickerListenerRegistrars
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TickerFetchScheduler(
        private val tickerListenerRegistrars: TickerListenerRegistrars,
        private val profitCache: TwoLegArbitrageProfitCache,
        private val executorService: ScheduledExecutorService
) {
    companion object : KLogging()

    fun scheduleFetchingTickers() {
        logger.info { "Will fetch ticker pairs every 10 seconds" }
        executorService.scheduleAtFixedRate({
            try {
                tickerListenerRegistrars.fetchTickersAndNotifyListeners()
                profitCache.removeTooOldProfits()
            } catch (e: Exception) {
                logger.error(e) { "Could not fetch ticker pairs" }
            }
        }, 0, 10, TimeUnit.SECONDS)
    }
}