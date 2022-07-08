package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.orderbook.OrderBook
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration
import java.time.temporal.ChronoUnit

class StaleOrdersDetectorTest {

    @ParameterizedTest
    @CsvSource(
        "20,110,111,,,130,false",
        "20,110,111,,,131,true",
        "20,110,111,109,,130,true",
        "20,110,111,,109,130,true",
        "20,110,111,109,109,130,true",
        "20,110,111,110,110,130,false",
    )
    fun shouldOrdersBeTooOld(
        maxTickerAgeMillis: Long,
        firstOrderBookReceivedAtMillis: Long,
        secondOrderBookReceivedAtMillis: Long,
        firstOrderBookExchangeTimestampMillis: Long?,
        secondOrderBookExchangeTimestampMillis: Long?,
        currentTimeMillis: Long,
        shouldBeTooOld: Boolean,
    ) {
        // given
        val maxOrderBookAge = Duration.of(maxTickerAgeMillis, ChronoUnit.MILLIS)
        val orderBookPairWithTooOldOrders = OrderBookPair(
            first = OrderBook(
                buyOrders = emptyList(),
                sellOrders = emptyList(),
                receivedAtMillis = firstOrderBookReceivedAtMillis,
                exchangeTimestampMillis = firstOrderBookExchangeTimestampMillis,
            ),
            second = OrderBook(
                buyOrders = emptyList(),
                sellOrders = emptyList(),
                receivedAtMillis = secondOrderBookReceivedAtMillis,
                exchangeTimestampMillis = secondOrderBookExchangeTimestampMillis,
            )
        )
        val tested = StaleOrderBooksDetector(currentTimeMillisFunction = { currentTimeMillis }, maxOrderBookAge = maxOrderBookAge)
        // when
        val ordersAreTooOld = tested.orderBooksAreTooOld(orderBookPairWithTooOldOrders)
        // then
        assertThat(ordersAreTooOld).isEqualTo(shouldBeTooOld)
    }
}
