package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.orderbook.OrderBookAveragePrice
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class TwoLegArbitrageRelativeProfitCalculatorWithoutMetadataTest {
    private val tested = TwoLegArbitrageRelativeProfitCalculatorWithoutMetadata()

    @ParameterizedTest
    @CsvSource(
        "18.0,20.0,0.1",
        "20.0,18.0,-0.1",
    )
    fun shouldCalculateProfitWhenBuyAtFirstExchange(firstOrderBookAverageSellPrice: BigDecimal, secondOrderBookAverageBuyPrice: BigDecimal, expectedRelativeProfit: BigDecimal) {
        // when
        val profit = tested.getProfitBuyAtFirstExchangeSellAtSecond(
            currencyPairWithExchangePair = mock(),
            firstOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = firstOrderBookAverageSellPrice,
                baseCurrencyAmount = mock()
            ),
            secondOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = secondOrderBookAverageBuyPrice,
                baseCurrencyAmount = mock()
            ),
        )
        // then
        assertThat(profit).isEqualByComparingTo(expectedRelativeProfit)
    }

    @ParameterizedTest
    @CsvSource(
        "10.0,8.0,0.2",
        "8.0,10.0,-0.2",
    )
    fun shouldCalculateProfitWhenBuyAtSecondExchange(firstOrderBookAverageBuyPrice: BigDecimal, secondOrderBookAverageSellPrice: BigDecimal, expectedRelativeProfit: BigDecimal) {
        // when
        val profit = tested.getProfitBuyAtSecondExchangeSellAtFirst(
            currencyPairWithExchangePair = mock(),
            firstOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = firstOrderBookAverageBuyPrice,
                baseCurrencyAmount = mock()
            ),
            secondOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = secondOrderBookAverageSellPrice,
                baseCurrencyAmount = mock()
            ),
        )
        // then
        assertThat(profit).isEqualByComparingTo(expectedRelativeProfit)
    }
}