package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.orderbook.OrderBookExchangeOrder
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerFetcher
import mu.KLogging
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.math.RoundingMode.HALF_EVEN
import java.time.Duration
import java.time.temporal.ChronoUnit

class TwoLegOrderBookArbitrageProfitCalculator(
        private val priceService: PriceService,
        private val tickerFetcher: TickerFetcher,
        private val orderBookUsdAmountThresholds: List<BigDecimal>,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis,
        private val maxAgeOfFirstOrderInOrderBookMs: Long = Duration.of(2, ChronoUnit.HOURS).toMillis()
) {
    companion object : KLogging()


    private val minimum = Int.MIN_VALUE.toBigDecimal()
    private val maximum = Int.MAX_VALUE.toBigDecimal()

    fun calculateProfit(currencyPairWithExchangePair: CurrencyPairWithExchangePair, orderBookPair: OrderBookPair): TwoLegOrderBookArbitrageProfit? {
        logger.debug { "Calculating profit for $currencyPairWithExchangePair" }
        val currentTimeMillis = currentTimeMillis()

        val firstExchangeTicker = tickerFetcher.getCachedTicker(currencyPairWithExchangePair.exchangePair.firstExchange, currencyPairWithExchangePair.currencyPair)
        val secondExchangeTicker = tickerFetcher.getCachedTicker(currencyPairWithExchangePair.exchangePair.secondExchange, currencyPairWithExchangePair.currencyPair)
        if (oneOfTickersIsTooOld(firstExchangeTicker, secondExchangeTicker, currentTimeMillis)) {
            return null
        }

        if (ordersAreTooOld(orderBookPair.first.buyOrders, currentTimeMillis)
                || ordersAreTooOld(orderBookPair.first.sellOrders, currentTimeMillis)
                || ordersAreTooOld(orderBookPair.second.buyOrders, currentTimeMillis)
                || ordersAreTooOld(orderBookPair.second.sellOrders, currentTimeMillis)) {
            return null
        }

        return try {
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
                    firstOrderBookBuyPrice?.averagePrice ?: minimum <= ZERO
                            || firstOrderBookSellPrice?.averagePrice ?: minimum <= ZERO
                            || secondOrderBookBuyPrice?.averagePrice ?: minimum <= ZERO
                            || secondOrderBookSellPrice?.averagePrice ?: minimum <= ZERO
                    -> null
                    firstOrderBookBuyPrice?.averagePrice ?: minimum > secondOrderBookSellPrice?.averagePrice ?: maximum ->
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
                    secondOrderBookBuyPrice?.averagePrice ?: minimum > firstOrderBookSellPrice?.averagePrice ?: maximum ->
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

            val usd24hVolumeAtFirstExchange = priceService.getUsdValue(firstExchangeTicker.currencyPair.counter, firstExchangeTicker.counterCurrency24hVolume)
            val usd24hVolumeAtSecondExchange = priceService.getUsdValue(secondExchangeTicker.currencyPair.counter, secondExchangeTicker.counterCurrency24hVolume)

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

    private fun ordersAreTooOld(orders: List<OrderBookExchangeOrder>, currentTimeMillis: Long): Boolean {
        with(orders) {
            return if (isEmpty()) {
                return true
            } else {
                val whenNoTimestampOrderIsNotTooOld = first().timestamp?.toEpochMilli() ?: currentTimeMillis
                currentTimeMillis - whenNoTimestampOrderIsNotTooOld > maxAgeOfFirstOrderInOrderBookMs
            }
        }
    }

    private fun oneOfTickersIsTooOld(ticker1: Ticker, ticker2: Ticker, currentTimeMillis: Long): Boolean {
        return (currentTimeMillis - (ticker1.timestamp?.toEpochMilli() ?: 0L) > maxAgeOfFirstOrderInOrderBookMs ||
                currentTimeMillis - (ticker2.timestamp?.toEpochMilli() ?: 0L) > maxAgeOfFirstOrderInOrderBookMs)

    }

}