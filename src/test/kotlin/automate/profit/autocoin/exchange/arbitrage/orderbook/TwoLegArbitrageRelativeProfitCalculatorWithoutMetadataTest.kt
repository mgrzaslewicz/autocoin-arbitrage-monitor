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
        "101.0,102.0,18.0,20.0,0.1,101.0",
        "102.0,102.5,20.0,18.0,-0.1,102.0",
    )
    fun shouldCalculateProfitWhenBuyAtFirstExchange(
        firstOrderBookBaseCurrencyAmount: BigDecimal,
        secondOrderBookBaseCurrencyAmount: BigDecimal,
        firstOrderBookAverageSellPrice: BigDecimal,
        secondOrderBookAverageBuyPrice: BigDecimal,
        expectedRelativeProfit: BigDecimal,
        expectedBaseCurrencyAmountAfterTransfer: BigDecimal,
    ) {
        // when
        val profit = tested.getProfitBuyAtFirstExchangeSellAtSecond(
            currencyPairWithExchangePair = mock(),
            firstOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = firstOrderBookAverageSellPrice,
                baseCurrencyAmount = firstOrderBookBaseCurrencyAmount
            ),
            secondOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = secondOrderBookAverageBuyPrice,
                baseCurrencyAmount = secondOrderBookBaseCurrencyAmount
            ),
        )
        // then
        assertThat(profit.relativeProfit).isEqualByComparingTo(expectedRelativeProfit)
        assertThat(profit.baseCurrencyAmountBeforeTransfer).isEqualByComparingTo(expectedBaseCurrencyAmountAfterTransfer)
        assertThat(profit.baseCurrencyAmountAfterTransfer).isEqualByComparingTo(profit.baseCurrencyAmountBeforeTransfer)
    }

    @ParameterizedTest
    @CsvSource(
        "1000.5,1001.0,10.0,8.0,0.2,1000.5",
        "1001.5,1001.3,8.0,10.0,-0.2,1001.3",
    )
    fun shouldCalculateProfitWhenBuyAtSecondExchange(
        firstOrderBookBaseCurrencyAmount: BigDecimal,
        secondOrderBookBaseCurrencyAmount: BigDecimal,
        firstOrderBookAverageBuyPrice: BigDecimal,
        secondOrderBookAverageSellPrice: BigDecimal,
        expectedRelativeProfit: BigDecimal,
        expectedBaseCurrencyAmountAfterTransfer: BigDecimal,
    ) {
        // when
        val profit = tested.getProfitBuyAtSecondExchangeSellAtFirst(
            currencyPairWithExchangePair = mock(),
            firstOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = firstOrderBookAverageBuyPrice,
                baseCurrencyAmount = firstOrderBookBaseCurrencyAmount,
            ),
            secondOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = secondOrderBookAverageSellPrice,
                baseCurrencyAmount = secondOrderBookBaseCurrencyAmount,
            ),
        )
        // then
        assertThat(profit.relativeProfit).isEqualByComparingTo(expectedRelativeProfit)
        assertThat(profit.baseCurrencyAmountBeforeTransfer).isEqualByComparingTo(expectedBaseCurrencyAmountAfterTransfer)
        assertThat(profit.baseCurrencyAmountAfterTransfer).isEqualByComparingTo(profit.baseCurrencyAmountBeforeTransfer)
    }
}