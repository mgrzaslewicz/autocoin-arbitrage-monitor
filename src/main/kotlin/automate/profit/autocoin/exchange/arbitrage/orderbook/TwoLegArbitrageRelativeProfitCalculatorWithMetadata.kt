package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.ExchangeMetadataService
import automate.profit.autocoin.exchange.orderbook.OrderBookAveragePrice
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import java.math.BigDecimal
import java.math.RoundingMode

interface TransactionFeeAmountFunction {
    fun apply(exchange: SupportedExchange, currencyPair: CurrencyPair, amount: BigDecimal): BigDecimal?
}

interface WithdrawalFeeAmountFunction {
    fun apply(exchange: SupportedExchange, currency: String, amount: BigDecimal): BigDecimal?
}

/**
 * Takes into account:
 * - withdrawal fee
 * - trade fee
 * - TODO currency withdrawal status at exchange
 */
class TwoLegArbitrageRelativeProfitCalculatorWithMetadata(
    private val firstExchangeTransactionFeeAmountFunction: TransactionFeeAmountFunction,
    private val firstExchangeWithdrawalFeeAmountFunction: WithdrawalFeeAmountFunction,
    private val secondExchangeTransactionFeeAmountFunction: TransactionFeeAmountFunction,
    private val secondExchangeWithdrawalFeeAmountFunction: WithdrawalFeeAmountFunction,
) : TwoLegArbitrageRelativeProfitCalculator {

    class DefaultBuilder(
        private val metadataService: ExchangeMetadataService,
    ) {
        val transactionFeeAmountFunction = object : TransactionFeeAmountFunction {
            override fun apply(exchange: SupportedExchange, currencyPair: CurrencyPair, amount: BigDecimal): BigDecimal? {
                val takerFeeRange = metadataService.getMetadata(exchangeName = exchange.exchangeName, currencyPair = currencyPair)
                    .transactionFeeRanges
                    .takerFeeRange(amount)
                return when {
                    takerFeeRange?.feeRatio != null -> amount.multiply(takerFeeRange.feeRatio)
                    takerFeeRange?.feeAmount != null -> takerFeeRange.feeAmount
                    else -> null
                }
            }
        }
        val withdrawalFeeAmountFunction = object : WithdrawalFeeAmountFunction {
            override fun apply(exchange: SupportedExchange, currency: String, amount: BigDecimal): BigDecimal? {
                val currencyMetadata = metadataService.getMetadata(exchangeName = exchange.exchangeName, currency = currency)
                return when {
                    currencyMetadata.minWithdrawalAmount?.compareTo(amount) == 1 -> null
                    else -> currencyMetadata.withdrawalFeeAmount
                }
            }
        }

        fun build(): TwoLegArbitrageRelativeProfitCalculator = TwoLegArbitrageRelativeProfitCalculatorWithMetadata(
            firstExchangeTransactionFeeAmountFunction = transactionFeeAmountFunction,
            secondExchangeTransactionFeeAmountFunction = transactionFeeAmountFunction,
            firstExchangeWithdrawalFeeAmountFunction = withdrawalFeeAmountFunction,
            secondExchangeWithdrawalFeeAmountFunction = withdrawalFeeAmountFunction,
        )
    }

    override fun getProfitBuyAtSecondExchangeSellAtFirst(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookBuyPrice: OrderBookAveragePrice,
        secondOrderBookSellPrice: OrderBookAveragePrice
    ): BigDecimal {
        val relativeProfitWithoutFees = firstOrderBookBuyPrice.averagePrice.divide(secondOrderBookSellPrice.averagePrice, RoundingMode.HALF_EVEN) - BigDecimal.ONE
        val baseCurrencyAmountMinusTransactionFeeAtSecondExchange = secondOrderBookSellPrice.baseCurrencyAmount - (secondExchangeTransactionFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            currencyPair = currencyPairWithExchangePair.currencyPair,
            amount = secondOrderBookSellPrice.baseCurrencyAmount
        ) ?: BigDecimal.ZERO)
        val baseCurrencyAmountMinusSecondExchangeWithdrawalFee = baseCurrencyAmountMinusTransactionFeeAtSecondExchange - (secondExchangeWithdrawalFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            currency = currencyPairWithExchangePair.currencyPair.base,
            amount = secondOrderBookSellPrice.baseCurrencyAmount
        ) ?: BigDecimal.ZERO)
        val baseCurrencyAmountMinusTransactionFeeAtFirstExchange = baseCurrencyAmountMinusSecondExchangeWithdrawalFee - (firstExchangeTransactionFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.secondExchange,
            currencyPair = currencyPairWithExchangePair.currencyPair,
            amount = baseCurrencyAmountMinusSecondExchangeWithdrawalFee
        ) ?: BigDecimal.ZERO)
        val amountWithAllFeesAppliedPlusProfit =
            baseCurrencyAmountMinusTransactionFeeAtFirstExchange.plus(baseCurrencyAmountMinusTransactionFeeAtFirstExchange.multiply(relativeProfitWithoutFees))
        return amountWithAllFeesAppliedPlusProfit
            .divide(secondOrderBookSellPrice.baseCurrencyAmount, RoundingMode.HALF_EVEN)
            .minus(BigDecimal.ONE)
    }

    override fun getProfitBuyAtFirstExchangeSellAtSecond(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookSellPrice: OrderBookAveragePrice,
        secondOrderBookBuyPrice: OrderBookAveragePrice
    ): BigDecimal {
        val relativeProfitWithoutFees = secondOrderBookBuyPrice.averagePrice.divide(firstOrderBookSellPrice.averagePrice, RoundingMode.HALF_EVEN) - BigDecimal.ONE
        val baseCurrencyAmountMinusTransactionFeeAtFirstExchange = firstOrderBookSellPrice.baseCurrencyAmount - (firstExchangeTransactionFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            currencyPair = currencyPairWithExchangePair.currencyPair,
            amount = firstOrderBookSellPrice.baseCurrencyAmount
        ) ?: BigDecimal.ZERO)
        val baseCurrencyAmountMinusFirstExchangeWithdrawalFee = baseCurrencyAmountMinusTransactionFeeAtFirstExchange - (firstExchangeWithdrawalFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            currency = currencyPairWithExchangePair.currencyPair.base,
            amount = firstOrderBookSellPrice.baseCurrencyAmount
        ) ?: BigDecimal.ZERO)
        val baseCurrencyAmountMinusTransactionFeeAtSecondExchange = baseCurrencyAmountMinusFirstExchangeWithdrawalFee - (secondExchangeTransactionFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.secondExchange,
            currencyPair = currencyPairWithExchangePair.currencyPair,
            amount = baseCurrencyAmountMinusFirstExchangeWithdrawalFee
        ) ?: BigDecimal.ZERO)
        val amountWithAllFeesAppliedPlusProfit = baseCurrencyAmountMinusTransactionFeeAtSecondExchange.plus(baseCurrencyAmountMinusTransactionFeeAtSecondExchange.multiply(relativeProfitWithoutFees))
        return amountWithAllFeesAppliedPlusProfit
            .divide(firstOrderBookSellPrice.baseCurrencyAmount, RoundingMode.HALF_EVEN)
            .minus(BigDecimal.ONE)
    }

}