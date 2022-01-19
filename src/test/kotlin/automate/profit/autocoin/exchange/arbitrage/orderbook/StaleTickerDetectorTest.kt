package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerPair
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class StaleTickerDetectorTest {

    @Test
    fun shouldTickersBeTooOld() {
        // given
        val maxAgeOfFirstTicker = Duration.of(1L, ChronoUnit.HOURS)
        val tickerPairWithTooOldOrders = TickerPair(
            first = mock<Ticker>().apply { whenever(this.timestamp).thenReturn(Instant.ofEpochMilli(1L)) },
            second = mock<Ticker>().apply { whenever(this.timestamp).thenReturn(Instant.ofEpochMilli(1L)) },
        )
        val tested = StaleTickerDetector(currentTimeMillisFunction = { maxAgeOfFirstTicker.toMillis() + 2 }, maxAgeOfFirstTicker = maxAgeOfFirstTicker)
        // when
        val tickerAreTooOld = tested.oneOfTickersIsTooOld(tickerPairWithTooOldOrders)
        // then
        assertThat(tickerAreTooOld).isTrue
    }

    @Test
    fun shouldTickersNotBeTooOld() {
        // given
        val maxAgeOfFirstTicker = Duration.of(1L, ChronoUnit.HOURS)
        val tickerPairWithTooOldOrders = TickerPair(
            first = mock<Ticker>().apply { whenever(this.timestamp).thenReturn(Instant.ofEpochMilli(1L)) },
            second = mock<Ticker>().apply { whenever(this.timestamp).thenReturn(Instant.ofEpochMilli(1L)) },
        )
        val tested = StaleTickerDetector(currentTimeMillisFunction = { maxAgeOfFirstTicker.toMillis() + 1 }, maxAgeOfFirstTicker = maxAgeOfFirstTicker)
        // when
        val tickerAreTooOld = tested.oneOfTickersIsTooOld(tickerPairWithTooOldOrders)
        // then
        assertThat(tickerAreTooOld).isFalse
    }

    @Test
    fun shouldTickersBeTooOldWhenNoTimestamp() {
        // given
        val maxAgeOfFirstTicker = Duration.of(1L, ChronoUnit.HOURS)
        val tickerPairWithTooOldOrders = TickerPair(first = mock(), second = mock())
        val tested = StaleTickerDetector(currentTimeMillisFunction = { maxAgeOfFirstTicker.toMillis() + 1 }, maxAgeOfFirstTicker = maxAgeOfFirstTicker)
        // when
        val tickerAreTooOld = tested.oneOfTickersIsTooOld(tickerPairWithTooOldOrders)
        // then
        assertThat(tickerAreTooOld).isTrue
    }
}