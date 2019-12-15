package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.orderbook.OrderBookExchangeOrder
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerFetcher
import mu.KLogging
import java.math.BigDecimal
import java.math.BigDecimal.ONE
import java.math.RoundingMode.HALF_EVEN
import java.time.Duration
import java.time.temporal.ChronoUnit

class TwoLegOrderBookArbitrageProfitCalculator(
        private val priceService: PriceService,
        private val tickerFetcher: TickerFetcher,
        private val orderBookUsdAmountThresholds: List<BigDecimal>,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis,
        private val maxAgeOfTickerMs: Long = Duration.of(2, ChronoUnit.HOURS).toMillis()
) {
    companion object : KLogging()


    private val minimum = Int.MIN_VALUE.toBigDecimal()
    private val maximum = Int.MAX_VALUE.toBigDecimal()

    fun calculateProfit(currencyPairWithExchangePair: CurrencyPairWithExchangePair, orderBookPair: OrderBookPair): TwoLegOrderBookArbitrageProfit? {
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
                orderBookPair.first.getWeightedAverageBuyPrice(usdPrice, it)
            }
            val secondOrderBookAverageBuyPrices = orderBookUsdAmountThresholds.map {
                orderBookPair.second.getWeightedAverageBuyPrice(usdPrice, it)
            }
            val opportunities = orderBookUsdAmountThresholds.mapIndexed { index, usdDepthTo ->
                when {
                    firstOrderBookAverageBuyPrices[index] ?: minimum > secondOrderBookAverageBuyPrices[index] ?: maximum ->
                        TwoLegOrderBookArbitrageOpportunity(
                                sellPrice = firstOrderBookAverageBuyPrices[index]!!,
                                sellAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                                buyAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                                buyPrice = secondOrderBookAverageBuyPrices[index]!!,
                                relativeProfit = firstOrderBookAverageBuyPrices[index]!!.divide(secondOrderBookAverageBuyPrices[index]!!, HALF_EVEN) - ONE,
                                usdDepthUpTo = usdDepthTo
                        )
                    secondOrderBookAverageBuyPrices[index] ?: minimum > firstOrderBookAverageBuyPrices[index] ?: maximum ->
                        TwoLegOrderBookArbitrageOpportunity(
                                sellPrice = secondOrderBookAverageBuyPrices[index]!!,
                                sellAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                                buyAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                                buyPrice = firstOrderBookAverageBuyPrices[index]!!,
                                relativeProfit = secondOrderBookAverageBuyPrices[index]!!.divide(firstOrderBookAverageBuyPrices[index]!!, HALF_EVEN) - ONE,
                                usdDepthUpTo = usdDepthTo
                        )

                    else -> null
                }
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
            logger.error(e) { "Could not calculate two leg arbitrage profit" }
            null
        }
    }

    private fun ordersAreTooOld(orders: List<OrderBookExchangeOrder>, currentTimeMillis: Long): Boolean {
        with(orders) {
            return if (isEmpty()) {
                return true
            } else {
                val whenNoTimestampOrderIsNotTooOld = first().timestamp?.toEpochMilli() ?: currentTimeMillis
                currentTimeMillis - whenNoTimestampOrderIsNotTooOld > maxAgeOfTickerMs
            }
        }
    }

    private fun oneOfTickersIsTooOld(ticker1: Ticker, ticker2: Ticker, currentTimeMillis: Long): Boolean {
        return (currentTimeMillis - (ticker1.timestamp?.toEpochMilli() ?: 0L) > maxAgeOfTickerMs ||
                currentTimeMillis - (ticker2.timestamp?.toEpochMilli() ?: 0L) > maxAgeOfTickerMs)

    }

}