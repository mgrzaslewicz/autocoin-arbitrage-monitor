package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.currency.CurrencyPair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class TickerPairCacheTest {
    private val currencyPair = CurrencyPair.of("A/B")
    private val exchangePair = ExchangePair(BITTREX, BINANCE)
    private val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
    private val firstTicker = Ticker(currencyPair = currencyPair, ask = BigDecimal("1.001"), bid = BigDecimal("1.0011"), timestamp = Instant.ofEpochMilli(1))

    @Test
    fun shouldGetAndCleanTickerCurrencyPairs() {
        // given
        val tickerPairCache = TickerPairCache()
        // when
        tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(firstTicker, firstTicker))
        tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(firstTicker.copy(timestamp = Instant.ofEpochMilli(2)), firstTicker.copy(timestamp = Instant.ofEpochMilli(2))))
        tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(firstTicker.copy(timestamp = Instant.ofEpochMilli(2)), firstTicker.copy(timestamp = Instant.ofEpochMilli(3))))
        tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(firstTicker.copy(timestamp = Instant.ofEpochMilli(4)), firstTicker.copy(timestamp = Instant.ofEpochMilli(4))))
        tickerPairCache.addTickerPair(currencyPairWithExchangePair, TickerPair(firstTicker.copy(timestamp = Instant.ofEpochMilli(6)), firstTicker.copy(timestamp = Instant.ofEpochMilli(6))))
        // then
        val tickerCurrencyPairs = tickerPairCache.getAndCleanTickerCurrencyPairs(currencyPairWithExchangePair)
        assertThat(tickerCurrencyPairs).hasSize(5)
        assertThat(tickerPairCache.getTickerCurrencyPairs(currencyPairWithExchangePair)).isEmpty()
    }

}