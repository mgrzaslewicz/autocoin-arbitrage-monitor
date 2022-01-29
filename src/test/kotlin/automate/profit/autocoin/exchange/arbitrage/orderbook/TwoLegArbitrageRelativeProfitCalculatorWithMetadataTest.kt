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
                baseCurrencyAmount = "10.51".toBigDecimal()
            ),
        )
        // then
        assertThat(profit.relativeProfit).isEqualByComparingTo("0.03".toBigDecimal())
        assertThat(profit.baseCurrencyAmountBeforeTransfer).isEqualByComparingTo("10.5".toBigDecimal())
        assertThat(profit.baseCurrencyAmountAfterTransfer).isEqualByComparingTo("10.5".toBigDecimal())
    }

    @ParameterizedTest
    @CsvSource(
        "5.01,5.0,10.0,11.0,0.005,0.01,0.08902750,5.0,4.9850",
        "1001.0,1000.0,10.0,11.0,0.02,20.0,0.035958,1000.0,981.000",
        "1000.5,1000.0,10.0,11.0,0.02,20.0,0.035419,1000.0,980.500",
        "4.99,5.0,10.0,11.0,0.1,0.0,-0.1090,4.99,4.491", // transaction fee eating profit that was initially 10%
        "4.99,5.0,10.0,11.0,0.0,1.0,-0.12044,4.99,4.99", // withdrawal fee eating profit that was initially 10%
        "4.99,5.0,10.0,11.0,0.1,1.0,-0.30740,4.99,4.491", // both transaction and withdrawal fee eating profit that was initially 10%
    )
    fun shouldCalculateProfitTakingFeesIntoAccountWhenBuyAtFirstExchange(
        firstOrderBookBaseCurrencyAmount: BigDecimal,
        secondOrderBookBaseCurrencyAmount: BigDecimal,
        firstOrderBookAverageSellPrice: BigDecimal,
        secondOrderBookAverageBuyPrice: BigDecimal,
        transactionFeeRatio: BigDecimal,
        withdrawalFeeAmount: BigDecimal,
        expectedRelativeProfit: BigDecimal,
        expectedBaseCurrencyAmountBeforeTransfer: BigDecimal,
        expectedBaseCurrencyAmountAfterTransfer: BigDecimal,
    ) {
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
                averagePrice = firstOrderBookAverageSellPrice,
                baseCurrencyAmount = firstOrderBookBaseCurrencyAmount,
            ),
            secondOrderBookBuyPrice = OrderBookAveragePrice(
                averagePrice = secondOrderBookAverageBuyPrice,
                baseCurrencyAmount = secondOrderBookBaseCurrencyAmount,
            ),
        )
        // then
        assertThat(profit.relativeProfit).isEqualByComparingTo(expectedRelativeProfit)
        assertThat(profit.baseCurrencyAmountBeforeTransfer).isEqualByComparingTo(expectedBaseCurrencyAmountBeforeTransfer)
        assertThat(profit.baseCurrencyAmountAfterTransfer).isEqualByComparingTo(expectedBaseCurrencyAmountAfterTransfer)
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
                baseCurrencyAmount = "10.49".toBigDecimal()
            ),
        )
        // then
        assertThat(profit.relativeProfit).isEqualByComparingTo("0.03".toBigDecimal())
        assertThat(profit.baseCurrencyAmountBeforeTransfer).isEqualByComparingTo("10.49".toBigDecimal())
        assertThat(profit.baseCurrencyAmountAfterTransfer).isEqualByComparingTo("10.49".toBigDecimal())
    }

    @ParameterizedTest
    @CsvSource(
        "5.0,5.01,11.0,10.0,0.005,0.01,0.08683850,5.0,4.9401750",
        "50.0,50.1,11.0,10.0,0.005,0.01,0.08880860,50.0,49.4913000",
        "500.0,501.2,11.0,10.0,0.005,0.01,0.08900561,500.0,495.0025500",
        "5.0,4.98,11.0,10.0,0.1,0.0,-0.1090,4.98,4.0338", // transaction fee eating profit that was initially 10%
        "5.0,5.05,11.0,10.0,0.0,1.0,-0.1200,5.0,4.0", // withdrawal fee eating profit that was initially 10%
        "5.0,5.0,11.0,10.0,0.1,1.0,-0.3070,5.0,3.150", // both transaction and withdrawal fee eating profit that was initially 10%
    )
    fun shouldCalculateProfitTakingFeesIntoAccountWhenBuyAtSecondExchange(
        firstOrderBookBaseCurrencyAmount: BigDecimal,
        secondOrderBookBaseCurrencyAmount: BigDecimal,
        firstOrderBookAverageBuyPrice: BigDecimal,
        secondOrderBookAverageSellPrice: BigDecimal,
        transactionFeeRatio: BigDecimal,
        withdrawalFeeAmount: BigDecimal,
        expectedRelativeProfit: BigDecimal,
        expectedBaseCurrencyAmountBeforeTransfer: BigDecimal,
        expectedBaseCurrencyAmountAfterTransfer: BigDecimal,
    ) {
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
        assertThat(profit.baseCurrencyAmountBeforeTransfer).isEqualByComparingTo(expectedBaseCurrencyAmountBeforeTransfer)
        assertThat(profit.baseCurrencyAmountAfterTransfer).isEqualByComparingTo(expectedBaseCurrencyAmountAfterTransfer)
    }

}