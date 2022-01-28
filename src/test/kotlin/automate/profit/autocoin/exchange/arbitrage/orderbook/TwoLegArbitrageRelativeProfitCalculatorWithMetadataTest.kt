package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.*
import automate.profit.autocoin.exchange.orderbook.OrderBookAveragePrice
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class TwoLegArbitrageRelativeProfitCalculatorWithMetadataTest {
    val nullTransactionFeeAmountFunction = object : TransactionFeeAmountFunction {
        override fun apply(exchange: SupportedExchange, currencyPair: CurrencyPair, amount: BigDecimal): BigDecimal? = null
    }

    val nullWithdrawalFeeAmountFunction = object : WithdrawalFeeAmountFunction {
        override fun apply(exchange: SupportedExchange, currency: String, amount: BigDecimal): BigDecimal? = null
    }

    @Test
    fun shouldBuyAtFirstExchangeAndSellAtSecond() {
        // given
        val tested = TwoLegArbitrageRelativeProfitCalculatorWithMetadata(
            firstExchangeTransactionFeeAmountFunction = nullTransactionFeeAmountFunction,
            secondExchangeTransactionFeeAmountFunction = nullTransactionFeeAmountFunction,
            firstExchangeWithdrawalFeeAmountFunction = nullWithdrawalFeeAmountFunction,
            secondExchangeWithdrawalFeeAmountFunction = nullWithdrawalFeeAmountFunction,
        )
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
    fun shouldCalculateProfitWhenBuyAtFirstExchangeWhenNoFeesAvailable() {
        // given
        val tested = TwoLegArbitrageRelativeProfitCalculatorWithMetadata(
            firstExchangeTransactionFeeAmountFunction = nullTransactionFeeAmountFunction,
            secondExchangeTransactionFeeAmountFunction = nullTransactionFeeAmountFunction,
            firstExchangeWithdrawalFeeAmountFunction = nullWithdrawalFeeAmountFunction,
            secondExchangeWithdrawalFeeAmountFunction = nullWithdrawalFeeAmountFunction,
        )
        // when
        val profit = tested.getProfitBuyAtFirstExchangeSellAtSecond(
            currencyPairWithExchangePair = CurrencyPairWithExchangePair(
                currencyPair = CurrencyPair.of("ETH/BTC"),
                ExchangePair(firstExchange = SupportedExchange.BITTREX, secondExchange = SupportedExchange.BINANCE)
            ),
            firstOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = "9.55".toBigDecimal(),
                baseCurrencyAmount = "10.5".toBigDecimal()
            ),
            secondOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = "9.85".toBigDecimal(),
                baseCurrencyAmount = "10.5".toBigDecimal()
            ),
        )
        // then
        assertThat(profit.compareTo("0.03".toBigDecimal())).isZero
    }

    @ParameterizedTest
    @CsvSource(
        // transactionFeeRatio, withdrawalFeeAmount, expectedRelativeProfit
        "0.008,0.02,0.07819118",
        "0.008,1.50,-0.23847078"
    )
    fun shouldCalculateProfitTakingFeesIntoAccountWhenBuyAtFirstExchange(transactionFeeRatio: BigDecimal, withdrawalFeeAmount: BigDecimal, expectedRelativeProfit: BigDecimal) {
        // given
        val intValueWhichDoesNotMatter = 0
        val bigDecimalValueWhichDoesNotMatter = BigDecimal.ZERO
        val metadataService: ExchangeMetadataService = mock<ExchangeMetadataService>().apply {
            whenever(this.getMetadata(any(), eq(CurrencyPair.of("ETH/BTC")))).thenReturn(
                CurrencyPairMetadata(
                    amountScale = intValueWhichDoesNotMatter,
                    priceScale = intValueWhichDoesNotMatter,
                    minimumAmount = bigDecimalValueWhichDoesNotMatter,
                    maximumAmount = bigDecimalValueWhichDoesNotMatter,
                    minimumOrderValue = bigDecimalValueWhichDoesNotMatter,
                    maximumPriceMultiplierDown = bigDecimalValueWhichDoesNotMatter,
                    maximumPriceMultiplierUp = bigDecimalValueWhichDoesNotMatter,
                    buyFeeMultiplier = bigDecimalValueWhichDoesNotMatter,
                    transactionFeeRanges = TransactionFeeRanges(
                        makerFees = emptyList(),
                        takerFees = listOf(
                            TransactionFeeRange(
                                beginAmount = BigDecimal.ZERO,
                                feeRatio = transactionFeeRatio
                            )
                        )
                    )
                )
            )
            whenever(this.getMetadata(any(), any<String>())).thenReturn(
                CurrencyMetadata(
                    scale = intValueWhichDoesNotMatter,
                    withdrawalFeeAmount = withdrawalFeeAmount,
                    minWithdrawalAmount = null
                )
            )
        }
        val tested = TwoLegArbitrageRelativeProfitCalculatorWithMetadata.DefaultBuilder(metadataService = metadataService).build()
        // when
        val profit = tested.getProfitBuyAtFirstExchangeSellAtSecond(
            currencyPairWithExchangePair = CurrencyPairWithExchangePair(
                currencyPair = CurrencyPair.of("ETH/BTC"),
                ExchangePair(firstExchange = SupportedExchange.BITTREX, secondExchange = SupportedExchange.BINANCE)
            ),
            firstOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = "10.00".toBigDecimal(),
                baseCurrencyAmount = "5.1".toBigDecimal()
            ),
            secondOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = "11.0".toBigDecimal(),
                baseCurrencyAmount = "5.1".toBigDecimal()
            ),
        )
        // then
        assertThat(profit).isEqualByComparingTo(expectedRelativeProfit)
    }

    @Test
    fun shouldBuyBuyAtSecondExchangeAndSellAtFirst() {
        // given
        val tested = TwoLegArbitrageRelativeProfitCalculatorWithMetadata(
            firstExchangeTransactionFeeAmountFunction = nullTransactionFeeAmountFunction,
            secondExchangeTransactionFeeAmountFunction = nullTransactionFeeAmountFunction,
            firstExchangeWithdrawalFeeAmountFunction = nullWithdrawalFeeAmountFunction,
            secondExchangeWithdrawalFeeAmountFunction = nullWithdrawalFeeAmountFunction,
        )
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
    fun shouldCalculateProfitWhenBuyAtSecondExchangeWhenNoFeesAvailable() {
        // given
        val tested = TwoLegArbitrageRelativeProfitCalculatorWithMetadata(
            firstExchangeTransactionFeeAmountFunction = nullTransactionFeeAmountFunction,
            secondExchangeTransactionFeeAmountFunction = nullTransactionFeeAmountFunction,
            firstExchangeWithdrawalFeeAmountFunction = nullWithdrawalFeeAmountFunction,
            secondExchangeWithdrawalFeeAmountFunction = nullWithdrawalFeeAmountFunction,
        )
        // when
        val profit = tested.getProfitBuyAtSecondExchangeSellAtFirst(
            currencyPairWithExchangePair = CurrencyPairWithExchangePair(
                currencyPair = CurrencyPair.of("ETH/BTC"),
                ExchangePair(firstExchange = SupportedExchange.BITTREX, secondExchange = SupportedExchange.BINANCE)
            ),
            firstOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = "9.85".toBigDecimal(),
                baseCurrencyAmount = "10.5".toBigDecimal()
            ),
            secondOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = "9.55".toBigDecimal(),
                baseCurrencyAmount = "10.5".toBigDecimal()
            ),
        )
        // then
        assertThat(profit.compareTo("0.03".toBigDecimal())).isZero
    }

    @ParameterizedTest
    @CsvSource(
        // transactionFeeRatio, withdrawalFeeAmount, expectedRelativeProfit
        "0.005,0.01,0.018749702",
        "0.005,1.00,-0.077879012"
    )
    fun shouldCalculateProfitTakingFeesIntoAccountWhenBuyAtSecondExchange(transactionFeeRatio: BigDecimal, withdrawalFeeAmount: BigDecimal, expectedRelativeProfit: BigDecimal) {
        // given
        val intValueWhichDoesNotMatter = 0
        val bigDecimalValueWhichDoesNotMatter = BigDecimal.ZERO
        val metadataService: ExchangeMetadataService = mock<ExchangeMetadataService>().apply {
            whenever(this.getMetadata(any(), eq(CurrencyPair.of("ETH/BTC")))).thenReturn(
                CurrencyPairMetadata(
                    amountScale = intValueWhichDoesNotMatter,
                    priceScale = intValueWhichDoesNotMatter,
                    minimumAmount = bigDecimalValueWhichDoesNotMatter,
                    maximumAmount = bigDecimalValueWhichDoesNotMatter,
                    minimumOrderValue = bigDecimalValueWhichDoesNotMatter,
                    maximumPriceMultiplierDown = bigDecimalValueWhichDoesNotMatter,
                    maximumPriceMultiplierUp = bigDecimalValueWhichDoesNotMatter,
                    buyFeeMultiplier = bigDecimalValueWhichDoesNotMatter,
                    transactionFeeRanges = TransactionFeeRanges(
                        makerFees = emptyList(),
                        takerFees = listOf(
                            TransactionFeeRange(
                                beginAmount = BigDecimal.ZERO,
                                feeRatio = transactionFeeRatio
                            )
                        )
                    )
                )
            )
            whenever(this.getMetadata(any(), any<String>())).thenReturn(
                CurrencyMetadata(
                    scale = intValueWhichDoesNotMatter,
                    withdrawalFeeAmount = withdrawalFeeAmount,
                    minWithdrawalAmount = null
                )
            )
        }
        val tested = TwoLegArbitrageRelativeProfitCalculatorWithMetadata.DefaultBuilder(metadataService = metadataService).build()
        // when
        val profit = tested.getProfitBuyAtSecondExchangeSellAtFirst(
            currencyPairWithExchangePair = CurrencyPairWithExchangePair(
                currencyPair = CurrencyPair.of("ETH/BTC"),
                ExchangePair(firstExchange = SupportedExchange.BITTREX, secondExchange = SupportedExchange.BINANCE)
            ),
            firstOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = "9.85".toBigDecimal(),
                baseCurrencyAmount = "10.5".toBigDecimal()
            ),
            secondOrderBookSellPrice = OrderBookAveragePrice(
                averagePrice = "9.55".toBigDecimal(),
                baseCurrencyAmount = "10.5".toBigDecimal()
            ),
        )
        // then
        assertThat(profit).isEqualByComparingTo(expectedRelativeProfit)
    }

}