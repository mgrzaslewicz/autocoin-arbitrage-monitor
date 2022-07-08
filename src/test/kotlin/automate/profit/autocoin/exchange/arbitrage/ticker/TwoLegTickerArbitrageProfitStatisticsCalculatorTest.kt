package automate.profit.autocoin.exchange.arbitrage.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.FileTickerPairRepository
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerPair
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.data.Percentage
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant

class TwoLegTickerArbitrageProfitStatisticsCalculatorTest {

    private val currencyPair = CurrencyPair.of("A/B")
    private val exchangePair = ExchangePair(SupportedExchange.BITTREX, SupportedExchange.BINANCE)
    private val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
    private val usd24hVolume = BigDecimal(2000.0)
    private val pricesService = mock<PriceService>().apply {
        whenever(getUsdValue(eq("B"), any())).thenReturn(usd24hVolume)
    }
    private val currentFixedTimeMs = 10L
    private val freshTickerTime = Instant.ofEpochMilli(15L)
    private val twoLegArbitrageProfitCalculator = TwoLegTickerArbitrageProfitCalculator(pricesService, { currentFixedTimeMs })
    private val volumeDoesNotMatter = BigDecimal.ONE
    private val tickerPairs = listOf(
            tickerPair(10.0, 11.0, 9.5, 10.5), // relative profit 0.1
            tickerPair(11.0, 12.0, 10.5, 11.5) // relative profit 0
    )

    private fun tickerPair(buyA: Double, sellA: Double, buyB: Double, sellB: Double): TickerPair {
        return TickerPair(
                first = Ticker(
                        currencyPair = currencyPair,
                        timestamp = freshTickerTime,
                        ask = sellA.toBigDecimal(),
                        bid = buyA.toBigDecimal(),
                        baseCurrency24hVolume = volumeDoesNotMatter,
                        counterCurrency24hVolume = volumeDoesNotMatter
                ),
                second = Ticker(
                        currencyPair = currencyPair,
                        timestamp = freshTickerTime,
                        ask = sellB.toBigDecimal(),
                        bid = buyB.toBigDecimal(),
                        baseCurrency24hVolume = volumeDoesNotMatter,
                        counterCurrency24hVolume = volumeDoesNotMatter
                )
        )
    }

    @Test
    fun shouldCalculateStatistics() {
        // given
        val tested = TwoLegArbitrageProfitStatisticsCalculator(mock<FileTickerPairRepository>().apply {
            whenever(this@apply.getTickerPairs(currencyPairWithExchangePair)).thenReturn(tickerPairs)
        }, twoLegArbitrageProfitCalculator)
        // when
        val statistic = tested.calculateStatistic(currencyPairWithExchangePair)!!
        // then
        assertThat(statistic.average).isEqualTo(0.05.toBigDecimal())
        assertThat(statistic.min).isEqualTo(0.0.toBigDecimal())
        assertThat(statistic.max).isEqualTo(0.1.toBigDecimal())
        assertThat(statistic.profitOpportunityHistogram).hasSize(6)
        assertThat(statistic.profitOpportunityHistogram.first().relativeProfitThreshold).isCloseTo(BigDecimal(0.005), Percentage.withPercentage(0.1))
        assertThat(statistic.profitOpportunityHistogram.first().count).isEqualTo(0)
        assertThat(statistic.profitOpportunityHistogram.last().relativeProfitThreshold).isCloseTo(BigDecimal(0.05), Percentage.withPercentage(0.1))
        assertThat(statistic.minUsd24hVolume).isEqualTo(usd24hVolume)
        assertThat(statistic.profitOpportunityHistogram.last().count).isEqualTo(1)

    }

}