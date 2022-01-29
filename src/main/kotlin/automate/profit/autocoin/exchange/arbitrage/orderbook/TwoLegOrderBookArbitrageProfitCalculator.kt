package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.orderbook.OrderBookAveragePrice
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.TickerPair
import mu.KLogging
import java.math.BigDecimal

data class TwoLegArbitrageRelativeProfit(
    val relativeProfit: BigDecimal,
    val baseCurrencyAmountBeforeTransfer: BigDecimal,
    val baseCurrencyAmountAfterTransfer: BigDecimal,
)

interface TwoLegArbitrageRelativeProfitCalculator {

    fun getProfitBuyAtSecondExchangeSellAtFirst(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookBuyPrice: OrderBookAveragePrice,
        secondOrderBookSellPrice: OrderBookAveragePrice
    ): TwoLegArbitrageRelativeProfit

    fun getProfitBuyAtFirstExchangeSellAtSecond(
        currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        firstOrderBookSellPrice: OrderBookAveragePrice,
        secondOrderBookBuyPrice: OrderBookAveragePrice
    ): TwoLegArbitrageRelativeProfit

}

/**
 * The way to distinguish how detailed arbitrage opportunities are (for free VS paid)
 */
enum class TwoLegArbitrageRelativeProfitGroup {
    ACCURATE_USING_METADATA,
    INACCURATE_NOT_USING_METADATA,
}

class TwoLegOrderBookArbitrageProfitCalculator(
    private val priceService: PriceService,
    private val orderBookUsdAmountThresholds: List<BigDecimal>,
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
    private val staleOrdersDetector: StaleOrdersDetector = StaleOrdersDetector(currentTimeMillisFunction = currentTimeMillisFunction),
    private val staleTickerDetector: StaleTickerDetector = StaleTickerDetector(currentTimeMillisFunction = currentTimeMillisFunction),
    private val relativeProfitCalculator: TwoLegArbitrageRelativeProfitCalculator,
    val profitGroup: TwoLegArbitrageRelativeProfitGroup
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
                return@map if (firstOrderBookBuyPrice == null || firstOrderBookSellPrice == null || secondOrderBookBuyPrice == null || secondOrderBookSellPrice == null) {
                    null
                } else {
                    val profitBuyAtSecondSellAtFirst =
                        relativeProfitCalculator.getProfitBuyAtSecondExchangeSellAtFirst(currencyPairWithExchangePair, firstOrderBookBuyPrice, secondOrderBookSellPrice)
                    if (profitBuyAtSecondSellAtFirst.relativeProfit > BigDecimal.ZERO) {
                        TwoLegOrderBookArbitrageOpportunity(
                            sellPrice = firstOrderBookBuyPrice.averagePrice,
                            sellAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                            baseCurrencyAmountAtSellExchange = profitBuyAtSecondSellAtFirst.baseCurrencyAmountBeforeTransfer,

                            buyPrice = secondOrderBookSellPrice.averagePrice,
                            buyAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                            baseCurrencyAmountAtBuyExchange = profitBuyAtSecondSellAtFirst.baseCurrencyAmountAfterTransfer,

                            relativeProfit = profitBuyAtSecondSellAtFirst.relativeProfit,
                            usdDepthUpTo = usdDepthTo
                        )
                    } else {
                       val profitBuyAtFirstSellAtSecond = relativeProfitCalculator.getProfitBuyAtFirstExchangeSellAtSecond(currencyPairWithExchangePair, firstOrderBookSellPrice, secondOrderBookBuyPrice)
                        if (profitBuyAtFirstSellAtSecond.relativeProfit > BigDecimal.ZERO) {
                            TwoLegOrderBookArbitrageOpportunity(
                                sellPrice = secondOrderBookBuyPrice.averagePrice,
                                sellAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                                baseCurrencyAmountAtSellExchange = profitBuyAtFirstSellAtSecond.baseCurrencyAmountBeforeTransfer,

                                buyPrice = firstOrderBookSellPrice.averagePrice,
                                buyAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                                baseCurrencyAmountAtBuyExchange = profitBuyAtFirstSellAtSecond.baseCurrencyAmountAfterTransfer,

                                relativeProfit = profitBuyAtFirstSellAtSecond.relativeProfit,
                                usdDepthUpTo = usdDepthTo
                            )
                        } else {
                            null
                        }
                    }
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
                calculatedAtMillis = currentTimeMillis,
            )
        } catch (e: Exception) {
            logger.error(e) { "Could not calculate two leg arbitrage profit for $currencyPairWithExchangePair" }
            null
        }
    }

}