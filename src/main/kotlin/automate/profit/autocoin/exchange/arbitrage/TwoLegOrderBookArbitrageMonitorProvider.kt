package automate.profit.autocoin.exchange.arbitrage

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageMonitor
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitOpportunityCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.metrics.MetricsService

class TwoLegOrderBookArbitrageMonitorProvider(
    private val profitCache: TwoLegOrderBookArbitrageProfitOpportunityCache,
    private val profitCalculator: TwoLegOrderBookArbitrageProfitCalculator,
    private val metricsService: MetricsService
) {
    fun getTwoLegOrderBookArbitrageMonitors(commonCurrencyPairsAtExchanges: Map<CurrencyPair, Set<ExchangePair>>): List<TwoLegOrderBookArbitrageMonitor> {
        return commonCurrencyPairsAtExchanges.flatMap {
            it.value.map { exchangePair ->
                TwoLegOrderBookArbitrageMonitor(
                    currencyPairWithExchangePair = CurrencyPairWithExchangePair(it.key, exchangePair),
                    profitCache = profitCache,
                    profitCalculator = profitCalculator,
                    metricsService = metricsService
                )
            }
        }
    }
}
