package automate.profit.autocoin.exchange.arbitrage.orderbook

import com.autocoin.exchangegateway.spi.exchange.orderbook.OrderBook
import kotlin.math.min

data class OrderBookPair(
    val first: OrderBook,
    val second: OrderBook
) {

    fun oldestOrderBookReceivedAtOrExchangeMillis(): Long {
        return min(first.receivedAtMillis, second.receivedAtMillis, first.exchangeTimestampMillis, second.exchangeTimestampMillis)
    }

    private fun min(long1: Long, long2: Long, long3: Long?, long4: Long?): Long {
        return min(min(long1, long2), min(long3 ?: Long.MAX_VALUE, long4 ?: Long.MAX_VALUE))
    }
}
