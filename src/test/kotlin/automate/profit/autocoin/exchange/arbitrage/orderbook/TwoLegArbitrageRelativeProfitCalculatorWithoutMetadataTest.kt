package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.orderbook.OrderBookAveragePrice
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class TwoLegArbitrageRelativeProfitCalculatorWithoutMetadataTest {
    private val tested = TwoLegArbitrageRelativeProfitCalculatorWithoutMetadata()

    @Test
    fun shouldBuyAtFirstExchangeAndSellAtSecond() {
        val sellPriceAtFirstExchange = "10.0".toBigDecimal()
        val priceBiggerThanSellPriceAtFirstExchange = sellPriceAtFirstExchange.plus(BigDecimal.ONE)
        // when
        val shouldBuyAtFirstExchangeAndSellAtSecond = tested.shouldBuyAtFirstExchangeAndSellAtSecond(
            currencyPairWithExchangePair = mock(),
            firstOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = sellPriceAtFirstExchange,
                baseCurrencyAmount = mock()
            ),
            secondOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = priceBiggerThanSellPriceAtFirstExchange,
                baseCurrencyAmount = mock()
            ),
        )
        // then
        assertThat(shouldBuyAtFirstExchangeAndSellAtSecond).isTrue
    }

    @Test
    fun shouldCalculateProfitWhenBuyAtFirstExchange() {
        // when
        val profit = tested.getProfitBuyAtFirstExchangeSellAtSecond(
            currencyPairWithExchangePair = mock(),
            firstOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = "9.55".toBigDecimal(),
                baseCurrencyAmount = mock()
            ),
            secondOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = "9.85".toBigDecimal(),
                baseCurrencyAmount = mock()
            ),
        )
        // then
        assertThat(profit).isEqualTo("0.03".toBigDecimal())
    }

    @Test
    fun shouldBuyBuyAtSecondExchangeAndSellAtFirst() {
        val sellPriceAtSecondExchange = "9.55".toBigDecimal()
        val buyPriceHigherThanSellPriceAtSecondExchange = sellPriceAtSecondExchange.plus(BigDecimal.ONE)
        // when
        val shouldBuyAtSecondExchange = tested.shouldBuyAtSecondExchangeAndSellAtFirst(
            currencyPairWithExchangePair = mock(),
            firstOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = buyPriceHigherThanSellPriceAtSecondExchange,
                baseCurrencyAmount = mock()
            ),
            secondOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = sellPriceAtSecondExchange,
                baseCurrencyAmount = mock()
            ),
        )
        // then
        assertThat(shouldBuyAtSecondExchange).isTrue
    }

    @Test
    fun shouldCalculateProfitWhenBuyAtSecondExchange() {
        // when
        val profit = tested.getProfitBuyAtSecondExchangeSellAtFirst(
            currencyPairWithExchangePair = mock(),
            firstOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = "9.85".toBigDecimal(),
                baseCurrencyAmount = mock()
            ),
            secondOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = "9.55".toBigDecimal(),
                baseCurrencyAmount = mock()
            ),
        )
        // then
        assertThat(profit).isEqualTo("0.03".toBigDecimal())
    }
}