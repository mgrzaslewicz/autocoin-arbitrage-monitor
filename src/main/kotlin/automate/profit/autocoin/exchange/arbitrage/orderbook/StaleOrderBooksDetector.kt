package automate.profit.autocoin.exchange.arbitrage.orderbook

import java.time.Duration
import java.time.temporal.ChronoUnit

class StaleOrderBooksDetector(
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
    maxOrderBookAge: Duration = Duration.of(15, ChronoUnit.MINUTES),
) {
    private val maxOrderBookAgeMillis: Long = maxOrderBookAge.toMillis()

    fun orderBooksAreTooOld(orderBookPair: OrderBookPair): Boolean {
        val currentTimeMillis = currentTimeMillisFunction()
        return currentTimeMillis - orderBookPair.oldestOrderBookReceivedAtOrExchangeMillis() > maxOrderBookAgeMillis
    }

}
