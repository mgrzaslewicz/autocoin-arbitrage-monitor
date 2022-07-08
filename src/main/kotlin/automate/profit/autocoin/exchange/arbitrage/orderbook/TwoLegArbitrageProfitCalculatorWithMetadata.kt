package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.ExchangeMetadataService
import automate.profit.autocoin.exchange.orderbook.OrderBookAveragePrice
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import java.math.BigDecimal
import java.math.RoundingMode

data class TransactionFeeAmount(
    val feeAmount: BigDecimal,
    /**
     * If true, no transaction fee was available in currency pair metadata and default value is used
     */
    val isDefaultFeeUsed: Boolean,
)

interface TransactionFeeAmountFunction {
    fun apply(exchange: SupportedExchange, currencyPair: CurrencyPair, amount: BigDecimal): TransactionFeeAmount
}

interface WithdrawalFeeAmountFunction {
    fun apply(exchange: SupportedExchange, currency: String, amount: BigDecimal): BigDecimal?
}

class CurrencyWithdrawalAndDepositStatusChecker(private val metadataService: ExchangeMetadataService) {
    /**
     * Mark as disabled only currencies that have false values for withdrawal or deposit status.
     * There are many null values for depositEnabled and withdrawalEnabled and that should be treated as lack of value,
     * not false value which will make opportunity not appear
     */
    fun areWithdrawalsAndDepositsDisabled(currencyPairWithExchangePair: CurrencyPairWithExchangePair): Boolean {
        val firstExchangeBaseCurrencyMetadata = metadataService.getCurrencyMetadata(
            exchangeName = currencyPairWithExchangePair.exchangePair.firstExchange.exchangeName,
            currency = currencyPairWithExchangePair.currencyPair.base
        )
        val firstExchangeCounterCurrencyMetadata = metadataService.getCurrencyMetadata(
            exchangeName = currencyPairWithExchangePair.exchangePair.firstExchange.exchangeName,
            currency = currencyPairWithExchangePair.currencyPair.counter
        )
        val firstExchangeBaseCurrencyDepositEnabled = firstExchangeBaseCurrencyMetadata?.depositEnabled
        val firstExchangeCounterCurrencyDepositEnabled = firstExchangeCounterCurrencyMetadata?.depositEnabled
        val firstExchangeBaseCurrencyWithdrawalEnabled = firstExchangeBaseCurrencyMetadata?.withdrawalEnabled
        val firstExchangeCounterCurrencyWithdrawalEnabled = firstExchangeCounterCurrencyMetadata?.withdrawalEnabled

        val secondExchangeBaseCurrencyMetadata = metadataService.getCurrencyMetadata(
            exchangeName = currencyPairWithExchangePair.exchangePair.secondExchange.exchangeName,
            currency = currencyPairWithExchangePair.currencyPair.base
        )
        val secondExchangeCounterCurrencyMetadata = metadataService.getCurrencyMetadata(
            exchangeName = currencyPairWithExchangePair.exchangePair.secondExchange.exchangeName,
            currency = currencyPairWithExchangePair.currencyPair.counter
        )
        val secondExchangeBaseCurrencyDepositEnabled = secondExchangeBaseCurrencyMetadata?.depositEnabled
        val secondExchangeCounterCurrencyDepositEnabled = secondExchangeCounterCurrencyMetadata?.depositEnabled
        val secondExchangeBaseCurrencyWithdrawalEnabled = secondExchangeBaseCurrencyMetadata?.withdrawalEnabled
        val secondExchangeCounterCurrencyWithdrawalEnabled = secondExchangeCounterCurrencyMetadata?.withdrawalEnabled

        return (firstExchangeBaseCurrencyDepositEnabled != null && !firstExchangeBaseCurrencyDepositEnabled) ||
                (firstExchangeCounterCurrencyDepositEnabled != null && !firstExchangeCounterCurrencyDepositEnabled) ||
                (firstExchangeBaseCurrencyWithdrawalEnabled != null && !firstExchangeBaseCurrencyWithdrawalEnabled) ||
                (firstExchangeCounterCurrencyWithdrawalEnabled != null && !firstExchangeCounterCurrencyWithdrawalEnabled) ||
                (secondExchangeBaseCurrencyDepositEnabled != null && !secondExchangeBaseCurrencyDepositEnabled) ||
                (secondExchangeCounterCurrencyDepositEnabled != null && !secondExchangeCounterCurrencyDepositEnabled) ||
                (secondExchangeBaseCurrencyWithdrawalEnabled != null && !secondExchangeBaseCurrencyWithdrawalEnabled) ||
                (secondExchangeCounterCurrencyWithdrawalEnabled != null && !secondExchangeCounterCurrencyWithdrawalEnabled)
    }
}


/**
 * Takes into account:
 * - withdrawal fee
 * - trade fee
 * - TODO currency withdrawal status at exchange
 */
class TwoLegArbitrageProfitCalculatorWithMetadata(
    private val firstExchangeTransactionFeeAmountFunction: TransactionFeeAmountFunction,
    private val firstExchangeWithdrawalFeeAmountFunction: WithdrawalFeeAmountFunction,
    private val secondExchangeTransactionFeeAmountFunction: TransactionFeeAmountFunction,
    private val secondExchangeWithdrawalFeeAmountFunction: WithdrawalFeeAmountFunction,
    private val currencyWithdrawalAndDepositStatusChecker: CurrencyWithdrawalAndDepositStatusChecker,
    private val transactionFeeRatioWhenNotAvailableInMetadata: BigDecimal,
) : TwoLegArbitrageProfitCalculator {
    private val noProfit = -BigDecimal.ONE

    class DefaultBuilder(
        private val metadataService: ExchangeMetadataService,
        private val transactionFeeRatioWhenNotAvailableInMetadata: BigDecimal,
    ) {
        val transactionFeeAmountFunction = object : TransactionFeeAmountFunction {
            override fun apply(exchange: SupportedExchange, currencyPair: CurrencyPair, amount: BigDecimal): TransactionFeeAmount {
                val takerFeeRange = metadataService.getCurrencyPairMetadata(
                    exchangeName = exchange.exchangeName,
                    currencyPair = currencyPair
                )
                    ?.transactionFeeRanges
                    ?.takerFeeRange(amount)

                var isDefaultTransactionFeeUsed = false;

                val feeAmount = when {
                    takerFeeRange?.feeRatio != null -> amount.multiply(takerFeeRange.feeRatio)
                    takerFeeRange?.feeAmount != null -> takerFeeRange.feeAmount!!
                    else -> {
                        isDefaultTransactionFeeUsed = true;
                        amount.multiply(transactionFeeRatioWhenNotAvailableInMetadata)
                    }
                }
                return TransactionFeeAmount(feeAmount = feeAmount, isDefaultFeeUsed = isDefaultTransactionFeeUsed)
            }
        }
        val withdrawalFeeAmountFunction = object : WithdrawalFeeAmountFunction {
            override fun apply(exchange: SupportedExchange, currency: String, amount: BigDecimal): BigDecimal? {
                val currencyMetadata = metadataService.getCurrencyMetadata(exchangeName = exchange.exchangeName, currency = currency)
                return when {
                    currencyMetadata == null -> null
                    currencyMetadata.minWithdrawalAmount?.compareTo(amount) == 1 -> null
                    else -> currencyMetadata.withdrawalFeeAmount
                }
            }
        }

        fun build(): TwoLegArbitrageProfitCalculator = TwoLegArbitrageProfitCalculatorWithMetadata(
            firstExchangeTransactionFeeAmountFunction = transactionFeeAmountFunction,
            firstExchangeWithdrawalFeeAmountFunction = withdrawalFeeAmountFunction,
            secondExchangeTransactionFeeAmountFunction = transactionFeeAmountFunction,
            secondExchangeWithdrawalFeeAmountFunction = withdrawalFeeAmountFunction,
            currencyWithdrawalAndDepositStatusChecker = CurrencyWithdrawalAndDepositStatusChecker(metadataService),
            transactionFeeRatioWhenNotAvailableInMetadata = transactionFeeRatioWhenNotAvailableInMetadata,
        )
    }

    override fun getProfitBuyAtSecondExchangeSellAtFirst(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookBuyPrice: OrderBookAveragePrice,
        secondOrderBookSellPrice: OrderBookAveragePrice
    ): TwoLegArbitrageProfit {
        val relativeProfitWithoutFees = when {
            currencyWithdrawalAndDepositStatusChecker.areWithdrawalsAndDepositsDisabled(currencyPairWithExchangePair) -> noProfit
            else -> firstOrderBookBuyPrice.averagePrice.divide(secondOrderBookSellPrice.averagePrice, RoundingMode.HALF_EVEN) - BigDecimal.ONE
        }
        val baseCurrencyAmountBeforeTransfer = firstOrderBookBuyPrice.baseCurrencyAmount.min(secondOrderBookSellPrice.baseCurrencyAmount)
        val transactionFeeAmountBeforeTransfer = secondExchangeTransactionFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            currencyPair = currencyPairWithExchangePair.currencyPair,
            amount = baseCurrencyAmountBeforeTransfer
        )
        val baseCurrencyAmountMinusTransactionFeeAtSecondExchange = baseCurrencyAmountBeforeTransfer - (transactionFeeAmountBeforeTransfer.feeAmount)
        val transferFeeAmount = secondExchangeWithdrawalFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            currency = currencyPairWithExchangePair.currencyPair.base,
            amount = baseCurrencyAmountMinusTransactionFeeAtSecondExchange
        )
        val baseCurrencyAmountMinusSecondExchangeWithdrawalFee = baseCurrencyAmountMinusTransactionFeeAtSecondExchange - (transferFeeAmount ?: BigDecimal.ZERO)
        val transactionFeeAmountAfterTransfer = firstExchangeTransactionFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.secondExchange,
            currencyPair = currencyPairWithExchangePair.currencyPair,
            amount = baseCurrencyAmountMinusSecondExchangeWithdrawalFee
        )
        val baseCurrencyAmountMinusTransactionFeeAtFirstExchange = baseCurrencyAmountMinusSecondExchangeWithdrawalFee - (transactionFeeAmountAfterTransfer.feeAmount)
        val amountWithAllFeesAppliedPlusProfit =
            baseCurrencyAmountMinusTransactionFeeAtFirstExchange.plus(baseCurrencyAmountMinusTransactionFeeAtFirstExchange.multiply(relativeProfitWithoutFees))
        val relativeProfit = amountWithAllFeesAppliedPlusProfit
            .divide(baseCurrencyAmountBeforeTransfer, RoundingMode.HALF_EVEN)
            .minus(BigDecimal.ONE)
        return TwoLegArbitrageProfit(
            relativeProfit = relativeProfit,
            baseCurrencyAmountBeforeTransfer = baseCurrencyAmountBeforeTransfer,
            baseCurrencyAmountAfterTransfer = baseCurrencyAmountMinusTransactionFeeAtFirstExchange,
            transactionFeeAmountBeforeTransfer = transactionFeeAmountBeforeTransfer.feeAmount,
            isDefaultTransactionFeeAmountBeforeTransferUsed = transactionFeeAmountBeforeTransfer.isDefaultFeeUsed,
            transferFeeAmount = transferFeeAmount,
            transactionFeeAmountAfterTransfer = transactionFeeAmountAfterTransfer.feeAmount,
            isDefaultTransactionFeeAmountAfterTransferUsed = transactionFeeAmountAfterTransfer.isDefaultFeeUsed,
        )
    }


    override fun getProfitBuyAtFirstExchangeSellAtSecond(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookSellPrice: OrderBookAveragePrice,
        secondOrderBookBuyPrice: OrderBookAveragePrice
    ): TwoLegArbitrageProfit {
        val relativeProfitWithoutFees = when {
            currencyWithdrawalAndDepositStatusChecker.areWithdrawalsAndDepositsDisabled(currencyPairWithExchangePair) -> noProfit
            else -> secondOrderBookBuyPrice.averagePrice.divide(firstOrderBookSellPrice.averagePrice, RoundingMode.HALF_EVEN) - BigDecimal.ONE
        }
        val baseCurrencyAmountBeforeTransfer = secondOrderBookBuyPrice.baseCurrencyAmount.min(firstOrderBookSellPrice.baseCurrencyAmount)
        val transactionFeeAmountBeforeTransfer = firstExchangeTransactionFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            currencyPair = currencyPairWithExchangePair.currencyPair,
            amount = baseCurrencyAmountBeforeTransfer
        )
        val baseCurrencyAmountMinusTransactionFeeAtFirstExchange = firstOrderBookSellPrice.baseCurrencyAmount - (transactionFeeAmountBeforeTransfer.feeAmount)
        val transferFeeAmount = firstExchangeWithdrawalFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            currency = currencyPairWithExchangePair.currencyPair.base,
            amount = baseCurrencyAmountMinusTransactionFeeAtFirstExchange
        )
        val baseCurrencyAmountMinusFirstExchangeWithdrawalFee = baseCurrencyAmountMinusTransactionFeeAtFirstExchange - (transferFeeAmount ?: BigDecimal.ZERO)
        val transactionFeeAmountAfterTransfer = secondExchangeTransactionFeeAmountFunction.apply(
            exchange = currencyPairWithExchangePair.exchangePair.secondExchange,
            currencyPair = currencyPairWithExchangePair.currencyPair,
            amount = baseCurrencyAmountMinusFirstExchangeWithdrawalFee
        )
        val baseCurrencyAmountMinusTransactionFeeAtSecondExchange = baseCurrencyAmountMinusFirstExchangeWithdrawalFee - (transactionFeeAmountAfterTransfer.feeAmount)
        val amountWithAllFeesAppliedPlusProfit =
            baseCurrencyAmountMinusTransactionFeeAtSecondExchange.plus(baseCurrencyAmountMinusTransactionFeeAtSecondExchange.multiply(relativeProfitWithoutFees))
        val relativeProfit = amountWithAllFeesAppliedPlusProfit
            .divide(baseCurrencyAmountBeforeTransfer, RoundingMode.HALF_EVEN)
            .minus(BigDecimal.ONE)
        return TwoLegArbitrageProfit(
            relativeProfit = relativeProfit,
            baseCurrencyAmountBeforeTransfer = baseCurrencyAmountBeforeTransfer,
            baseCurrencyAmountAfterTransfer = baseCurrencyAmountMinusTransactionFeeAtFirstExchange,
            transactionFeeAmountBeforeTransfer = transactionFeeAmountBeforeTransfer.feeAmount,
            isDefaultTransactionFeeAmountBeforeTransferUsed = transactionFeeAmountBeforeTransfer.isDefaultFeeUsed,
            transferFeeAmount = transferFeeAmount,
            transactionFeeAmountAfterTransfer = transactionFeeAmountAfterTransfer.feeAmount,
            isDefaultTransactionFeeAmountAfterTransferUsed = transactionFeeAmountAfterTransfer.isDefaultFeeUsed,
        )
    }

}
