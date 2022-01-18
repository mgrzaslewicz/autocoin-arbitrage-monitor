package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.orderbook.OrderBookExchangeOrder
import java.time.Duration
import java.time.temporal.ChronoUnit

class StaleOrdersDetector(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    maxAgeOfFirstOrderInOrderBook: Duration = Duration.of(2, ChronoUnit.HOURS),
) {
    private val maxAgeOfFirstOrderInOrderBookMs: Long = maxAgeOfFirstOrderInOrderBook.toMillis()

    fun ordersAreTooOld(orderBookPair: OrderBookPair): Boolean {
        val currentTimeMillis = currentTimeMillis()
        return (ordersAreTooOld(orderBookPair.first.buyOrders, currentTimeMillis)
                || ordersAreTooOld(orderBookPair.first.sellOrders, currentTimeMillis)
                || ordersAreTooOld(orderBookPair.second.buyOrders, currentTimeMillis)
                || ordersAreTooOld(orderBookPair.second.sellOrders, currentTimeMillis)
                )
    }

    private fun ordersAreTooOld(orders: List<OrderBookExchangeOrder>, currentTimeMillis: Long): Boolean {
        with(orders) {
            return if (isEmpty()) {
                return true
            } else {
                val orderTimestampOrCurrentMillis = first().timestamp?.toEpochMilli() ?: currentTimeMillis
                currentTimeMillis - orderTimestampOrCurrentMillis > maxAgeOfFirstOrderInOrderBookMs
            }
        }
    }
}