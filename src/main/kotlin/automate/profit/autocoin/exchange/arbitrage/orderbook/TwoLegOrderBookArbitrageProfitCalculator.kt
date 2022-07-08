package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.orderbook.OrderBookAveragePrice
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.TickerPair
import mu.KLogging
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.RoundingMode.HALF_EVEN

interface TwoLegArbitrageRelativeProfitCalculator {

    fun getProfitBuyAtSecondExchangeSellAtFirst(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookBuyPrice: OrderBookAveragePrice,
        secondOrderBookSellPrice: OrderBookAveragePrice
    ): BigDecimal

    fun getProfitBuyAtFirstExchangeSellAtSecond(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookSellPrice: OrderBookAveragePrice,
        secondOrderBookBuyPrice: OrderBookAveragePrice
    ): BigDecimal

    fun shouldBuyAtFirstExchangeAndSellAtSecond(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookSellPrice: OrderBookAveragePrice,
        secondOrderBookBuyPrice: OrderBookAveragePrice
    ): Boolean

    fun shouldBuyAtSecondExchangeAndSellAtFirst(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookBuyPrice: OrderBookAveragePrice,
        secondOrderBookSellPrice: OrderBookAveragePrice
    ): Boolean
}

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
        return firstOrderBookBuyPrice.averagePrice.divide(secondOrderBookSellPrice.averagePrice, HALF_EVEN) - ONE
    }

    override fun getProfitBuyAtFirstExchangeSellAtSecond(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookSellPrice: OrderBookAveragePrice,
        secondOrderBookBuyPrice: OrderBookAveragePrice
    ): BigDecimal {
        return secondOrderBookBuyPrice.averagePrice.divide(firstOrderBookSellPrice.averagePrice, HALF_EVEN) - ONE
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

class TwoLegOrderBookArbitrageProfitCalculator(
    private val priceService: PriceService,
    private val orderBookUsdAmountThresholds: List<BigDecimal>,
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
    private val staleOrdersDetector: StaleOrdersDetector = StaleOrdersDetector(currentTimeMillisFunction = currentTimeMillisFunction),
    private val staleTickerDetector: StaleTickerDetector = StaleTickerDetector(currentTimeMillisFunction = currentTimeMillisFunction),
    private val relativeProfitCalculator: TwoLegArbitrageRelativeProfitCalculator
) {
    companion object : KLogging()

    fun calculateProfit(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        orderBookPair: OrderBookPair,
        tickerPair: TickerPair
    ): TwoLegOrderBookArbitrageProfit? {
        logger.debug { "Calculating profit for $currencyPairWithExchangePair" }

        if (staleTickerDetector.oneOfTickersIsTooOld(tickerPair) || staleOrdersDetector.ordersAreTooOld(orderBookPair)) {
            return null
        }

        return try {
            val currentTimeMillis = currentTimeMillisFunction()
            val usdPrice = priceService.getUsdPrice(currencyPairWithExchangePair.currencyPair.counter)

            val opportunities = orderBookUsdAmountThresholds.map { usdDepthTo ->
                val firstOrderBookBuyPrice = orderBookPair.first.getWeightedAverageBuyPrice(otherCurrencyAmount = usdDepthTo, otherCurrencyPrice = usdPrice)
                val firstOrderBookSellPrice = orderBookPair.first.getWeightedAverageSellPrice(otherCurrencyAmount = usdDepthTo, otherCurrencyPrice = usdPrice)
                val secondOrderBookBuyPrice = orderBookPair.second.getWeightedAverageBuyPrice(otherCurrencyAmount = usdDepthTo, otherCurrencyPrice = usdPrice)
                val secondOrderBookSellPrice = orderBookPair.second.getWeightedAverageSellPrice(otherCurrencyAmount = usdDepthTo, otherCurrencyPrice = usdPrice)
                when {
                    (firstOrderBookBuyPrice == null || firstOrderBookSellPrice == null || secondOrderBookBuyPrice == null || secondOrderBookSellPrice == null) -> null
                    relativeProfitCalculator.shouldBuyAtSecondExchangeAndSellAtFirst(currencyPairWithExchangePair, firstOrderBookBuyPrice, secondOrderBookSellPrice) ->
                        TwoLegOrderBookArbitrageOpportunity(
                            sellPrice = firstOrderBookBuyPrice.averagePrice,
                            sellAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                            baseCurrencyAmountAtSellExchange = firstOrderBookBuyPrice.baseCurrencyAmount,

                            buyPrice = secondOrderBookSellPrice.averagePrice,
                            buyAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                            baseCurrencyAmountAtBuyExchange = secondOrderBookSellPrice.baseCurrencyAmount,

                            relativeProfit = relativeProfitCalculator.getProfitBuyAtSecondExchangeSellAtFirst(currencyPairWithExchangePair, firstOrderBookBuyPrice, secondOrderBookSellPrice),
                            usdDepthUpTo = usdDepthTo
                        )
                    relativeProfitCalculator.shouldBuyAtFirstExchangeAndSellAtSecond(currencyPairWithExchangePair, firstOrderBookSellPrice, secondOrderBookBuyPrice) ->
                        TwoLegOrderBookArbitrageOpportunity(
                            sellPrice = secondOrderBookBuyPrice.averagePrice,
                            sellAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                            baseCurrencyAmountAtSellExchange = secondOrderBookBuyPrice.baseCurrencyAmount,

                            buyPrice = firstOrderBookSellPrice.averagePrice,
                            buyAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                            baseCurrencyAmountAtBuyExchange = firstOrderBookSellPrice.baseCurrencyAmount,

                            relativeProfit = relativeProfitCalculator.getProfitBuyAtFirstExchangeSellAtSecond(currencyPairWithExchangePair, firstOrderBookSellPrice, secondOrderBookBuyPrice),
                            usdDepthUpTo = usdDepthTo
                        )
                    else -> null
                }
            }

            if (opportunities.all { it == null }) {
                logger.trace { "No profit found for $currencyPairWithExchangePair" }
                return null
            }

            val usd24hVolumeAtFirstExchange = priceService.getUsdValue(tickerPair.first.currencyPair.counter, tickerPair.first.counterCurrency24hVolume)
            val usd24hVolumeAtSecondExchange = priceService.getUsdValue(tickerPair.second.currencyPair.counter, tickerPair.second.counterCurrency24hVolume)

            return TwoLegOrderBookArbitrageProfit(
                currencyPairWithExchangePair = currencyPairWithExchangePair,
                usd24hVolumeAtFirstExchange = usd24hVolumeAtFirstExchange,
                usd24hVolumeAtSecondExchange = usd24hVolumeAtSecondExchange,
                orderBookArbitrageProfitHistogram = opportunities,
                calculatedAtMillis = currentTimeMillis
            )
        } catch (e: Exception) {
            logger.error(e) { "Could not calculate two leg arbitrage profit for $currencyPairWithExchangePair" }
            null
        }
    }

}