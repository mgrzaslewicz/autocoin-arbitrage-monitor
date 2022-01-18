package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.TickerPair
import mu.KLogging
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.math.RoundingMode.HALF_EVEN

class TwoLegOrderBookArbitrageProfitCalculator(
    private val priceService: PriceService,
    private val orderBookUsdAmountThresholds: List<BigDecimal>,
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    private val staleOrdersDetector: StaleOrdersDetector = StaleOrdersDetector(currentTimeMillis = currentTimeMillis),
    private val staleTickerDetector: StaleTickerDetector = StaleTickerDetector(currentTimeMillis = currentTimeMillis),
) {
    companion object : KLogging()

    private val minimum = Int.MIN_VALUE.toBigDecimal()
    private val maximum = Int.MAX_VALUE.toBigDecimal()

    fun calculateProfit(currencyPairWithExchangePair: CurrencyPairWithExchangePair, orderBookPair: OrderBookPair, tickerPair: TickerPair): TwoLegOrderBookArbitrageProfit? {
        logger.debug { "Calculating profit for $currencyPairWithExchangePair" }

        if (staleTickerDetector.oneOfTickersIsTooOld(tickerPair) || staleOrdersDetector.ordersAreTooOld(orderBookPair)) {
            return null
        }

        return try {
            val currentTimeMillis = currentTimeMillis()
            val firstExchangeTicker = tickerPair.first
            val secondExchangeTicker = tickerPair.second
            val usdPrice = priceService.getUsdPrice(currencyPairWithExchangePair.currencyPair.counter)

            val firstOrderBookAverageBuyPrices = orderBookUsdAmountThresholds.map {
                orderBookPair.first.getWeightedAverageBuyPrice(otherCurrencyAmount = it, otherCurrencyPrice = usdPrice)
            }
            val firstOrderBookAverageSellPrices = orderBookUsdAmountThresholds.map {
                orderBookPair.first.getWeightedAverageSellPrice(otherCurrencyAmount = it, otherCurrencyPrice = usdPrice)
            }
            val secondOrderBookAverageBuyPrices = orderBookUsdAmountThresholds.map {
                orderBookPair.second.getWeightedAverageBuyPrice(otherCurrencyAmount = it, otherCurrencyPrice = usdPrice)
            }
            val secondOrderBookAverageSellPrices = orderBookUsdAmountThresholds.map {
                orderBookPair.second.getWeightedAverageSellPrice(otherCurrencyAmount = it, otherCurrencyPrice = usdPrice)
            }
            val opportunities = orderBookUsdAmountThresholds.mapIndexed { index, usdDepthTo ->
                val firstOrderBookBuyPrice = firstOrderBookAverageBuyPrices[index]
                val firstOrderBookSellPrice = firstOrderBookAverageSellPrices[index]
                val secondOrderBookBuyPrice = secondOrderBookAverageBuyPrices[index]
                val secondOrderBookSellPrice = secondOrderBookAverageSellPrices[index]
                when {
                    (firstOrderBookBuyPrice?.averagePrice ?: minimum) <= ZERO
                            || (firstOrderBookSellPrice?.averagePrice ?: minimum) <= ZERO
                            || (secondOrderBookBuyPrice?.averagePrice ?: minimum) <= ZERO
                            || (secondOrderBookSellPrice?.averagePrice ?: minimum) <= ZERO
                    -> null
                    (firstOrderBookBuyPrice?.averagePrice ?: minimum) > (secondOrderBookSellPrice?.averagePrice ?: maximum) ->
                        TwoLegOrderBookArbitrageOpportunity(
                                sellPrice = firstOrderBookBuyPrice!!.averagePrice,
                                sellAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                                baseCurrencyAmountAtSellExchange = firstOrderBookBuyPrice.baseCurrencyAmount,

                                buyPrice = secondOrderBookSellPrice!!.averagePrice,
                                buyAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                                baseCurrencyAmountAtBuyExchange = secondOrderBookSellPrice.baseCurrencyAmount,

                                relativeProfit = firstOrderBookBuyPrice.averagePrice.divide(secondOrderBookSellPrice.averagePrice, HALF_EVEN) - ONE,
                                usdDepthUpTo = usdDepthTo
                        )
                    (secondOrderBookBuyPrice?.averagePrice ?: minimum) > (firstOrderBookSellPrice?.averagePrice ?: maximum) ->
                        TwoLegOrderBookArbitrageOpportunity(
                                sellPrice = secondOrderBookBuyPrice!!.averagePrice,
                                sellAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                                baseCurrencyAmountAtSellExchange = secondOrderBookBuyPrice.baseCurrencyAmount,

                                buyPrice = firstOrderBookSellPrice!!.averagePrice,
                                buyAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                                baseCurrencyAmountAtBuyExchange = firstOrderBookSellPrice.baseCurrencyAmount,

                                relativeProfit = secondOrderBookBuyPrice.averagePrice.divide(firstOrderBookSellPrice.averagePrice, HALF_EVEN) - ONE,
                                usdDepthUpTo = usdDepthTo
                        )

                    else -> null
                }
            }

            if (opportunities.all { it == null }) {
                logger.trace { "No profit found for $currencyPairWithExchangePair" }
                return null
            }

            val usd24hVolumeAtFirstExchange = priceService.getUsdValue(tickerPair.first.currencyPair.counter, firstExchangeTicker.counterCurrency24hVolume)
            val usd24hVolumeAtSecondExchange = priceService.getUsdValue(tickerPair.second.currencyPair.counter, secondExchangeTicker.counterCurrency24hVolume)

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