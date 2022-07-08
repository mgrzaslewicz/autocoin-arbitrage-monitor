package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.PriceResponseException
import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.orderbook.OrderBookAveragePrice
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.TickerPair
import automate.profit.autocoin.logger.PeriodicalLogger
import automate.profit.autocoin.metrics.MetricsService
import mu.KotlinLogging
import java.math.BigDecimal

data class TwoLegArbitrageRelativeProfit(
    val relativeProfit: BigDecimal,
    val baseCurrencyAmountBeforeTransfer: BigDecimal,
    val transactionFeeAmountBeforeTransfer: BigDecimal?,
    val transferFeeAmount: BigDecimal?,
    val baseCurrencyAmountAfterTransfer: BigDecimal,
    val transactionFeeAmountAfterTransfer: BigDecimal?,
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


class TwoLegOrderBookArbitrageProfitCalculator(
    private val priceService: PriceService,
    private val orderBookUsdAmountThresholds: List<BigDecimal>,
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
    private val staleOrdersDetector: StaleOrdersDetector = StaleOrdersDetector(currentTimeMillisFunction = currentTimeMillisFunction),
    private val staleTickerDetector: StaleTickerDetector = StaleTickerDetector(currentTimeMillisFunction = currentTimeMillisFunction),
    private val relativeProfitCalculator: TwoLegArbitrageRelativeProfitCalculator,
    private val metricsService: MetricsService,
) {
    companion object {
        private val logger = PeriodicalLogger(wrapped = KotlinLogging.logger {}).scheduleLogFlush()
    }

    /**
     * @param tickerPair needed to calculate currency volume in USD at both exchanges
     */
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
                            baseCurrencyAmountAtSellExchange = profitBuyAtSecondSellAtFirst.baseCurrencyAmountAfterTransfer,

                            buyPrice = secondOrderBookSellPrice.averagePrice,
                            buyAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                            baseCurrencyAmountAtBuyExchange = profitBuyAtSecondSellAtFirst.baseCurrencyAmountBeforeTransfer,

                            relativeProfit = profitBuyAtSecondSellAtFirst.relativeProfit,
                            profitUsd = profitBuyAtSecondSellAtFirst.relativeProfit.multiply(usdDepthTo),
                            usdDepthUpTo = usdDepthTo,

                            transactionFeeAmountBeforeTransfer = profitBuyAtSecondSellAtFirst.transactionFeeAmountBeforeTransfer,
                            transferFeeAmount = profitBuyAtSecondSellAtFirst.transferFeeAmount,
                            transactionFeeAmountAfterTransfer = profitBuyAtSecondSellAtFirst.transactionFeeAmountAfterTransfer,
                        )
                    } else {
                        val profitBuyAtFirstSellAtSecond =
                            relativeProfitCalculator.getProfitBuyAtFirstExchangeSellAtSecond(currencyPairWithExchangePair, firstOrderBookSellPrice, secondOrderBookBuyPrice)
                        if (profitBuyAtFirstSellAtSecond.relativeProfit > BigDecimal.ZERO) {
                            TwoLegOrderBookArbitrageOpportunity(
                                sellPrice = secondOrderBookBuyPrice.averagePrice,
                                sellAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                                baseCurrencyAmountAtSellExchange = profitBuyAtFirstSellAtSecond.baseCurrencyAmountBeforeTransfer,

                                buyPrice = firstOrderBookSellPrice.averagePrice,
                                buyAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                                baseCurrencyAmountAtBuyExchange = profitBuyAtFirstSellAtSecond.baseCurrencyAmountAfterTransfer,

                                relativeProfit = profitBuyAtFirstSellAtSecond.relativeProfit.multiply(usdDepthTo),
                                profitUsd = profitBuyAtFirstSellAtSecond.relativeProfit,
                                usdDepthUpTo = usdDepthTo,

                                transactionFeeAmountBeforeTransfer = profitBuyAtFirstSellAtSecond.transactionFeeAmountBeforeTransfer,
                                transferFeeAmount = profitBuyAtFirstSellAtSecond.transferFeeAmount,
                                transactionFeeAmountAfterTransfer = profitBuyAtFirstSellAtSecond.transactionFeeAmountAfterTransfer,
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
        } catch (e: PriceResponseException) {
            logger.frequentError(e) { "Could not calculate two leg arbitrage profit for $currencyPairWithExchangePair" }
            metricsService.recordNoUsdPriceForTwoLegProfitOpportunityCalculation(
                exchangePair = currencyPairWithExchangePair.exchangePair,
                currencyPair = currencyPairWithExchangePair.currencyPair,
                reasonTag = e.reasonTag
            )
            null
        } catch (e: Exception) {
            logger.frequentError(e) { "Could not calculate two leg arbitrage profit for $currencyPairWithExchangePair" }
            null
        }
    }

}
