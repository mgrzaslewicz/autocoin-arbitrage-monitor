package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.orderbook.OrderBookAveragePrice
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import java.math.BigDecimal
import java.math.RoundingMode

/**
 * Does not take into account:
 * - withdrawal fee
 * - trade fee
 * - if wallet at exchange allows withdrawal
 */
class TestTwoLegArbitrageRelativeProfitCalculator : TwoLegArbitrageRelativeProfitCalculator {
    override fun getProfitBuyAtSecondExchangeSellAtFirst(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookBuyPrice: OrderBookAveragePrice,
        secondOrderBookSellPrice: OrderBookAveragePrice
    ): TwoLegArbitrageRelativeProfit {
        val baseCurrencyAmountBeforeTransfer = secondOrderBookSellPrice.baseCurrencyAmount.min(firstOrderBookBuyPrice.baseCurrencyAmount)
        return TwoLegArbitrageRelativeProfit(
            relativeProfit = firstOrderBookBuyPrice.averagePrice.divide(secondOrderBookSellPrice.averagePrice, RoundingMode.HALF_EVEN) - BigDecimal.ONE,
            baseCurrencyAmountBeforeTransfer = baseCurrencyAmountBeforeTransfer,
            baseCurrencyAmountAfterTransfer = baseCurrencyAmountBeforeTransfer,
            transactionFeeAmountBeforeTransfer = null,
            transferFeeAmount = null,
            transactionFeeAmountAfterTransfer = null,
        )
    }

    override fun getProfitBuyAtFirstExchangeSellAtSecond(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookSellPrice: OrderBookAveragePrice,
        secondOrderBookBuyPrice: OrderBookAveragePrice
    ): TwoLegArbitrageRelativeProfit {
        val baseCurrencyAmountBeforeTransfer = secondOrderBookBuyPrice.baseCurrencyAmount.min(firstOrderBookSellPrice.baseCurrencyAmount)
        return TwoLegArbitrageRelativeProfit(
            relativeProfit = secondOrderBookBuyPrice.averagePrice.divide(firstOrderBookSellPrice.averagePrice, RoundingMode.HALF_EVEN) - BigDecimal.ONE,
            baseCurrencyAmountBeforeTransfer = baseCurrencyAmountBeforeTransfer,
            baseCurrencyAmountAfterTransfer = baseCurrencyAmountBeforeTransfer,
            transactionFeeAmountBeforeTransfer = null,
            transferFeeAmount = null,
            transactionFeeAmountAfterTransfer = null,
        )
    }

}
