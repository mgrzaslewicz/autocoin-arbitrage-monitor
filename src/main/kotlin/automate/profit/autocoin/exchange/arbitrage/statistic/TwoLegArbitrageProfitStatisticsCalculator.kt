package automate.profit.autocoin.exchange.arbitrage.statistic

import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCalculator
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair

class TwoLegArbitrageProfitStatisticsCalculator(
        private val twoLegOrderBookArbitrageProfitCalculator: TwoLegOrderBookArbitrageProfitCalculator
) {

    // TODO
    fun calculateAllStatistics(): List<TwoLegArbitrageProfitStatistic> {
        return emptyList()
    }

    // TODO
    fun calculateStatistic(currencyPairWithExchangePair: CurrencyPairWithExchangePair): TwoLegArbitrageProfitStatistic? {
        return null
    }

}