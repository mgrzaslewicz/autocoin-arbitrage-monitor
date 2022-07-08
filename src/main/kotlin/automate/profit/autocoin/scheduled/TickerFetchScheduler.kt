package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitCache
import automate.profit.autocoin.exchange.ticker.TickerListenerRegistrars
import mu.KLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TickerFetchScheduler(
        private val tickerListenerRegistrars: TickerListenerRegistrars,
        private val profitCache: TwoLegArbitrageProfitCache
) {
    companion object : KLogging()

    private val executorService = Executors.newScheduledThreadPool(1)

    fun scheduleFetchingTickers() {
        logger.info { "Will fetch ticker pairs every 10 seconds" }
        executorService.scheduleAtFixedRate({
            tickerListenerRegistrars.fetchTickersAndNotifyListeners()
            profitCache.removeTooOldProfits()
        }, 0, 10, TimeUnit.SECONDS)
    }
}