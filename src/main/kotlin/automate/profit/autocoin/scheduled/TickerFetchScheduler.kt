package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.ticker.TickerListenerRegistrars
import mu.KLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TickerFetchScheduler(private val tickerListenerRegistrars: TickerListenerRegistrars) {
    companion object : KLogging()

    private val executorService = Executors.newScheduledThreadPool(1)

    fun scheduleFetchingTickers() {
        TickerPairsSaveScheduler.logger.info { "Will fetch ticker pairs every 10 seconds" }
        executorService.scheduleAtFixedRate({ tickerListenerRegistrars.fetchTickersAndNotifyListeners() }, 0, 10, TimeUnit.SECONDS)
    }
}