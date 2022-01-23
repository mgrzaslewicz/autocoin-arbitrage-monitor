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
class TwoLegArbitrageRelativeProfitCalculatorWithoutMetadata: TwoLegArbitrageRelativeProfitCalculator {
    override fun getProfitBuyAtSecondExchangeSellAtFirst(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookBuyPrice: OrderBookAveragePrice,
        secondOrderBookSellPrice: OrderBookAveragePrice
    ): BigDecimal {
        return firstOrderBookBuyPrice.averagePrice.divide(secondOrderBookSellPrice.averagePrice, RoundingMode.HALF_EVEN) - BigDecimal.ONE
    }

    override fun getProfitBuyAtFirstExchangeSellAtSecond(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookSellPrice: OrderBookAveragePrice,
        secondOrderBookBuyPrice: OrderBookAveragePrice
    ): BigDecimal {
        return secondOrderBookBuyPrice.averagePrice.divide(firstOrderBookSellPrice.averagePrice, RoundingMode.HALF_EVEN) - BigDecimal.ONE
    }

    override fun shouldBuyAtFirstExchangeAndSellAtSecond(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookSellPrice: OrderBookAveragePrice,
        secondOrderBookBuyPrice: OrderBookAveragePrice
    ): Boolean {
        return secondOrderBookBuyPrice.averagePrice > firstOrderBookSellPrice.averagePrice
    }

    override fun shouldBuyAtSecondExchangeAndSellAtFirst(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookBuyPrice: OrderBookAveragePrice,
        secondOrderBookSellPrice: OrderBookAveragePrice
    ): Boolean {
        return firstOrderBookBuyPrice.averagePrice > secondOrderBookSellPrice.averagePrice
    }

}