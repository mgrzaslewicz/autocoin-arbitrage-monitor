package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerPair
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.Duration
import java.time.temporal.ChronoUnit

class StaleTickerDetectorTest {

    @ParameterizedTest
    @CsvSource(
        "20,110,111,,,130,false",
        "20,110,111,,,131,true",
        "20,110,111,109,,130,true",
        "20,110,111,,109,130,true",
        "20,110,111,109,109,130,true",
        "20,110,111,110,110,130,false",
    )
    fun shouldTickersBeTooOld(
        maxTickerAgeMillis: Long,
        firstExchangeTickerReceivedAtMillis: Long,
        secondExchangeTickerReceivedAtMillis: Long,
        firstExchangeTickerTimestamp: Long?,
        secondExchangeTickerTimestamp: Long?,
        currentTimeMillis: Long,
        shouldBeTooOld: Boolean,
    ) {
        // given
        val maxTickerAge = Duration.of(maxTickerAgeMillis, ChronoUnit.MILLIS)
        val tickerPairWithTooOldOrders = TickerPair(
            first = mock<Ticker>().apply {
                whenever(this.receivedAtMillis).thenReturn(firstExchangeTickerReceivedAtMillis)
                whenever(this.exchangeTimestampMillis).thenReturn(firstExchangeTickerTimestamp)
            },
            second = mock<Ticker>().apply {
                whenever(this.receivedAtMillis).thenReturn(secondExchangeTickerReceivedAtMillis)
                whenever(this.exchangeTimestampMillis).thenReturn(secondExchangeTickerTimestamp)
            }
        )
        val tested = StaleTickerDetector(currentTimeMillisFunction = { currentTimeMillis }, maxTickerAge = maxTickerAge)
        // when
        val tickerAreTooOld = tested.oneOfTickersIsTooOld(tickerPairWithTooOldOrders)
        // then
        assertThat(tickerAreTooOld).isEqualTo(shouldBeTooOld)
    }

}
