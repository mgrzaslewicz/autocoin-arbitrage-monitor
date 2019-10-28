package automate.profit.autocoin.exchange

import automate.profit.autocoin.exchange.ticker.TickerListenerRegistrars
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TickerFetchScheduler(private val tickerListenerRegistrars: TickerListenerRegistrars) {

    private val executorService = Executors.newScheduledThreadPool(1)

    fun scheduleFetchingTickers() {
        executorService.scheduleAtFixedRate({ tickerListenerRegistrars.fetchTickersAndNotifyListeners() }, 0, 10, TimeUnit.SECONDS)
    }
}