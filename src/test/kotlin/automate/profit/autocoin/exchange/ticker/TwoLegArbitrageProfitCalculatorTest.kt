package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.currency.CurrencyPair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class TwoLegArbitrageProfitCalculatorTest {
    private val currencyPair = CurrencyPair.of("A/B")
    private val exchangePair = ExchangePair(BITTREX, BINANCE)
    private val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
    private val twoLegArbitrageProfitCalculator = TwoLegArbitrageProfitCalculator()

    @Test
    fun shouldFindNoProfit() {
        // given
        val tickerPair = TickerPair(
                Ticker(currencyPair = currencyPair, ask = BigDecimal("1.001"), bid = BigDecimal("1.011"), timestamp = Instant.ofEpochMilli(1005)),
                Ticker(currencyPair = currencyPair, ask = BigDecimal("1.002"), bid = BigDecimal("1.0021"), timestamp = Instant.ofEpochMilli(1005))
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, tickerPair)
        // then
        assertThat(profit).isNull()
    }

    @Test
    fun shouldCalculateProfitWhenSellAtFirstExchange() {
        // given
        val tickerPair = TickerPair(
                Ticker(currencyPair = currencyPair, ask = BigDecimal("1.011"), bid = BigDecimal("1.001"), timestamp = Instant.ofEpochMilli(1005)),
                Ticker(currencyPair = currencyPair, ask = BigDecimal("1.0021"), bid = BigDecimal("1.002"), timestamp = Instant.ofEpochMilli(1005))
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, tickerPair)
        // then
        assertThat(profit?.buyAtExchange).isEqualTo(BINANCE)
        assertThat(profit?.sellAtExchange).isEqualTo(BITTREX)
        assertThat(profit?.sellPrice).isEqualTo(BigDecimal("1.011"))
        assertThat(profit?.buyPrice).isEqualTo(BigDecimal("1.002"))
        assertThat(profit?.relativeProfit).isEqualTo(BigDecimal("0.009")) // 0.0089820359 rounded
    }

    @Test
    fun shouldCalculateProfitWhenSellAtSecondExchange() {
        // given
        val tickerPair = TickerPair(
                Ticker(currencyPair = currencyPair, ask = BigDecimal("1.001"), bid = BigDecimal("1.001"), timestamp = Instant.ofEpochMilli(1005)),
                Ticker(currencyPair = currencyPair, ask = BigDecimal("1.0028"), bid = BigDecimal("1.003"), timestamp = Instant.ofEpochMilli(1005))
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, tickerPair)
        // then
        assertThat(profit?.buyAtExchange).isEqualTo(BITTREX)
        assertThat(profit?.sellAtExchange).isEqualTo(BINANCE)
        assertThat(profit?.sellPrice).isEqualTo(BigDecimal("1.0028"))
        assertThat(profit?.buyPrice).isEqualTo(BigDecimal("1.001"))
        assertThat(profit?.relativeProfit).isEqualTo(BigDecimal("0.0018")) // 0.0017982018 rounded
    }

}