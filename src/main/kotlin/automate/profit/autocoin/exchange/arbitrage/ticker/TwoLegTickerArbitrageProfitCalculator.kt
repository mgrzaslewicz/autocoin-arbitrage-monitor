package automate.profit.autocoin.exchange.arbitrage.ticker

import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.TickerPair
import mu.KLogging
import java.math.BigDecimal.ONE
import java.math.BigDecimal.ZERO
import java.math.RoundingMode.HALF_UP
import java.time.Duration
import java.time.temporal.ChronoUnit

class TwoLegTickerArbitrageProfitCalculator(
        private val priceService: PriceService,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis,
        private val maxAgeOfTickerMs: Long = Duration.of(2, ChronoUnit.MINUTES).toMillis()
) {
    companion object : KLogging()

    fun calculateProfits(currencyPairWithExchangePair: CurrencyPairWithExchangePair, tickerPairs: List<TickerPair>): List<TwoLegTickerArbitrageProfit> {
        return tickerPairs.mapNotNull { calculateProfit(currencyPairWithExchangePair, it) }
    }

    fun calculateProfit(currencyPairWithExchangePair: CurrencyPairWithExchangePair, tickerPair: TickerPair): TwoLegTickerArbitrageProfit? {
        return try {
            val currentTimeMillis = currentTimeMillis()
            when {
                oneOfTickersIsTooOld(tickerPair, currentTimeMillis) -> null
                tickerPair.first.bid > tickerPair.second.bid -> // sell on first, buy on second
                    TwoLegTickerArbitrageProfit(
                            currencyPair = currencyPairWithExchangePair.currencyPair,
                            exchangePair = currencyPairWithExchangePair.exchangePair,
                            sellAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                            buyAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                            buyPrice = tickerPair.second.bid,
                            sellPrice = tickerPair.first.bid,

                            usd24hVolumeAtBuyExchange = priceService.getUsdValue(tickerPair.second.currencyPair.counter, tickerPair.second.counterCurrency24hVolume),
                            usd24hVolumeAtSellExchange = priceService.getUsdValue(tickerPair.first.currencyPair.counter, tickerPair.first.counterCurrency24hVolume),

                            relativeProfit = if (tickerPair.second.bid > ZERO) tickerPair.first.bid.divide(tickerPair.second.bid, HALF_UP) - ONE else ZERO,
                            calculatedAtMillis = currentTimeMillis
                    )
                tickerPair.second.bid > tickerPair.first.bid -> // sell on second, buy on first
                    TwoLegTickerArbitrageProfit(
                            currencyPair = currencyPairWithExchangePair.currencyPair,
                            exchangePair = currencyPairWithExchangePair.exchangePair,
                            sellAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                            buyAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                            buyPrice = tickerPair.first.bid,
                            sellPrice = tickerPair.second.bid,

                            usd24hVolumeAtBuyExchange = priceService.getUsdValue(tickerPair.first.currencyPair.counter, tickerPair.first.counterCurrency24hVolume),
                            usd24hVolumeAtSellExchange = priceService.getUsdValue(tickerPair.second.currencyPair.counter, tickerPair.second.counterCurrency24hVolume),

                            relativeProfit = if (tickerPair.first.bid > ZERO) tickerPair.second.bid.divide(tickerPair.first.bid, HALF_UP) - ONE else ZERO,
                            calculatedAtMillis = currentTimeMillis
                    )
                else -> null
            }
        } catch (e: Exception) {
            logger.error(e) { "Could not calculate two leg arbitrage profit" }
            null
        }
    }

    private fun oneOfTickersIsTooOld(tickerPair: TickerPair, currentTimeMillis: Long): Boolean {
        return (currentTimeMillis - (tickerPair.first.timestamp?.toEpochMilli() ?: 0L) > maxAgeOfTickerMs ||
                currentTimeMillis - (tickerPair.first.timestamp?.toEpochMilli() ?: 0L) > maxAgeOfTickerMs)
    }

}