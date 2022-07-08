package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant
import java.util.*

class TickerPairCacheTest {
    private val currencyPair = CurrencyPair.of("A/B")
    private val exchangePair = ExchangePair(BITTREX, BINANCE)
    private val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
    private val firstTicker = Ticker(currencyPair = currencyPair, ask = BigDecimal("1.001"), bid = BigDecimal("1.0011"), timestamp = Instant.ofEpochMilli(1))

    @Test
    fun shouldRemoveTooOldTickerPair() {
        // given
        val timeMillis = ArrayDeque<Long>(listOf(1L, 2L, 3L, 4L, 7L))
        val tickerPairCache = TickerPairCache(ageOfOldestTickerPairToKeepMs = 5) { timeMillis.poll() }
        // when
        tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(firstTicker, firstTicker))
        tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(firstTicker.copy(timestamp = Instant.ofEpochMilli(2)), firstTicker.copy(timestamp = Instant.ofEpochMilli(2))))
        tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(firstTicker.copy(timestamp = Instant.ofEpochMilli(2)), firstTicker.copy(timestamp = Instant.ofEpochMilli(3))))
        tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(firstTicker.copy(timestamp = Instant.ofEpochMilli(4)), firstTicker.copy(timestamp = Instant.ofEpochMilli(4))))
        tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(firstTicker.copy(timestamp = Instant.ofEpochMilli(6)), firstTicker.copy(timestamp = Instant.ofEpochMilli(6))))
        // then
        val tickerCurrencyPairs = tickerPairCache.getTickerCurrencyPairs(currencyPairWithExchangePair)
        assertThat(tickerCurrencyPairs).hasSize(4)
        assertThat(tickerCurrencyPairs.last().first.timestamp?.toEpochMilli()).isEqualTo(2)
        assertThat(tickerCurrencyPairs.last().second.timestamp?.toEpochMilli()).isEqualTo(2)
    }

}