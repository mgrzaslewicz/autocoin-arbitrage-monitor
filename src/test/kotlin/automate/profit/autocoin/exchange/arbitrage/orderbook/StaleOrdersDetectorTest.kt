package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.orderbook.OrderBook
import automate.profit.autocoin.exchange.orderbook.OrderBookExchangeOrder
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class StaleOrdersDetectorTest {

    @Test
    fun shouldOrdersBeTooOld() {
        // given
        val sampleOrder = mock<OrderBookExchangeOrder>()
        val maxAgeOfOrder = Duration.of(1L, ChronoUnit.HOURS)
        val orderBookPairWithTooOldOrders = OrderBookPair(
            first = OrderBook(
                buyOrders = listOf(mock<OrderBookExchangeOrder>().apply { whenever(this.timestamp).thenReturn(Instant.ofEpochMilli(1L)) }),
                sellOrders = listOf(sampleOrder)
            ),
            second = OrderBook(
                buyOrders = listOf(sampleOrder),
                sellOrders = listOf(sampleOrder)
            )
        )
        val tested = StaleOrdersDetector(currentTimeMillisFunction = { maxAgeOfOrder.toMillis() + 2 }, maxAgeOfFirstOrderInOrderBook = maxAgeOfOrder)
        // when
        val ordersAreTooOld = tested.ordersAreTooOld(orderBookPairWithTooOldOrders)
        // then
        assertThat(ordersAreTooOld).isTrue
    }
}