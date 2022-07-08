package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import automate.profit.autocoin.exchange.orderbook.OrderBookListenerRegistrars
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class OrderBookFetchScheduler(
        private val orderBookListenerRegistrars: OrderBookListenerRegistrars,
        private val twoLegArbitrageProfitCache: TwoLegOrderBookArbitrageProfitCache,
        private val executorService: ScheduledExecutorService
) {
    companion object : KLogging()

    fun scheduleFetchingOrderBooks() {
        logger.info { "Will fetch order books every 10 seconds" }
        executorService.scheduleAtFixedRate({
            try {
                orderBookListenerRegistrars.fetchOrderBooksAndNotifyListeners()
            } catch (e: Exception) {
                logger.error(e) { "Could not order books" }
            }
        }, 0, 10, TimeUnit.SECONDS)
    }
}