package automate.profit.autocoin.exchange.arbitrage

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerPair
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class TwoLegArbitrageProfitCalculatorTest {
    private val currencyPair = CurrencyPair.of("X/Y")
    private val exchangeA = BITTREX
    private val exchangeB = BINANCE
    private val exchangePair = ExchangePair(exchangeA, exchangeB)
    private val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
    private val pricesService = mock<PriceService>().apply {
        whenever(getUsdValue(eq("Y"), any())).thenReturn(BigDecimal(2000.0))
    }
    private val twoLegArbitrageProfitCalculator = TwoLegArbitrageProfitCalculator(pricesService)
    private val doesNotMatter = Instant.now()
    private val volumeDoesNotMatter = BigDecimal.ONE

    private val priceToChangeInTest = BigDecimal.ONE
    private val sampleTicker = Ticker(currencyPair = currencyPair, ask = BigDecimal("1.011"), bid = priceToChangeInTest, timestamp = doesNotMatter, baseCurrency24hVolume = volumeDoesNotMatter, counterCurrency24hVolume = volumeDoesNotMatter)

    @Test
    fun shouldFindNoProfitWhenTickerHasNoTimestamp() {
        // given
        val buyPriceA = BigDecimal("1.0021")
        val buyPriceB = BigDecimal("1.001")
        val tickerWithoutTimestamp = sampleTicker.copy(bid = buyPriceA, timestamp = null)
        val tickerPair = TickerPair(tickerWithoutTimestamp, sampleTicker.copy(bid = buyPriceB))
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, tickerPair)
        // then
        assertThat(profit).isNull()

    }

    @Test
    fun shouldFindNoProfitWhenTickerTooOld() {
        // given
        val buyPriceA = BigDecimal("1.0021")
        val buyPriceB = BigDecimal("1.001")
        val tooOldTicker = sampleTicker.copy(
                bid = buyPriceA,
                timestamp = Instant.ofEpochMilli(Instant.now().toEpochMilli() - Duration.of(3, ChronoUnit.MINUTES).toMillis())
        )
        val tickerPair = TickerPair(tooOldTicker, sampleTicker.copy(bid = buyPriceB))
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, tickerPair)
        // then
        assertThat(profit).isNull()
    }


    @Test
    fun shouldFindNoProfitWhenSpreadTooSmall() {
        // given
        val theSameBuyPrice = BigDecimal("1.001")
        val tickerPair = TickerPair(sampleTicker.copy(bid = theSameBuyPrice), sampleTicker.copy(bid = theSameBuyPrice))
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, tickerPair)
        // then
        assertThat(profit).isNull()
    }

    @Test
    fun shouldSellAtExchangeA() {
        // given
        val buyPriceA = BigDecimal("1.0021")
        val buyPriceB = BigDecimal("1.001")
        val tickerPair = TickerPair(sampleTicker.copy(bid = buyPriceA), sampleTicker.copy(bid = buyPriceB))
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, tickerPair)
        // then
        with(SoftAssertions()) {
            assertThat(profit).isNotNull
            assertThat(profit?.buyAtExchange).isEqualTo(exchangeB)
            assertThat(profit?.sellAtExchange).isEqualTo(exchangeA)
            assertThat(profit?.sellPrice).isEqualTo(buyPriceA)
            assertThat(profit?.buyPrice).isEqualTo(buyPriceB)
            assertThat(profit?.relativeProfit).isEqualTo(BigDecimal("0.0011")) // 0.0089820359 rounded
            assertAll()
        }
    }

    @Test
    fun shouldSellAtExchangeB() {
        // given
        val buyPriceA = BigDecimal("1.001")
        val buyPriceB = BigDecimal("1.0021")
        val tickerPair = TickerPair(sampleTicker.copy(bid = buyPriceA), sampleTicker.copy(bid = buyPriceB))
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, tickerPair)
        // then
        with(SoftAssertions()) {
            assertThat(profit?.buyAtExchange).isEqualTo(exchangeA)
            assertThat(profit?.sellAtExchange).isEqualTo(exchangeB)
            assertThat(profit?.sellPrice).isEqualTo(buyPriceB)
            assertThat(profit?.buyPrice).isEqualTo(buyPriceA)
            assertThat(profit?.relativeProfit).isEqualTo(BigDecimal("0.0011")) // 0.0017982018 rounded
            assertAll()
        }
    }

}