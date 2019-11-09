package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfit
import java.math.BigDecimal.ONE
import java.math.RoundingMode.HALF_UP

class TwoLegArbitrageProfitCalculator(private val currentTimeMillis: () -> Long = System::currentTimeMillis) {

    fun calculateProfits(currencyPairWithExchangePair: CurrencyPairWithExchangePair, tickerPairs: List<TickerPair>): List<TwoLegArbitrageProfit> {
        return tickerPairs.mapNotNull { calculateProfit(currencyPairWithExchangePair, it) }
    }

    fun calculateProfit(currencyPairWithExchangePair: CurrencyPairWithExchangePair, tickerPair: TickerPair): TwoLegArbitrageProfit? {
        val currentTimeMillis = currentTimeMillis()
        return when {
            tickerPair.first.bid > tickerPair.second.bid -> // sell on first, buy on second
                TwoLegArbitrageProfit(
                        currencyPair = currencyPairWithExchangePair.currencyPair,
                        exchangePair = currencyPairWithExchangePair.exchangePair,
                        sellAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                        buyAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                        buyPrice = tickerPair.second.bid,
                        sellPrice = tickerPair.first.bid,
                        relativeProfit = tickerPair.first.bid.divide(tickerPair.second.bid, HALF_UP) - ONE,
                        calculatedAtMillis = currentTimeMillis
                )
            tickerPair.second.bid > tickerPair.first.bid -> // sell on second, buy on first
                TwoLegArbitrageProfit(
                        currencyPair = currencyPairWithExchangePair.currencyPair,
                        exchangePair = currencyPairWithExchangePair.exchangePair,
                        sellAtExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
                        buyAtExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
                        buyPrice = tickerPair.first.bid,
                        sellPrice = tickerPair.second.bid,
                        relativeProfit = tickerPair.second.bid.divide(tickerPair.first.bid, HALF_UP) - ONE,
                        calculatedAtMillis = currentTimeMillis
                )
            else -> null
        }
    }

}