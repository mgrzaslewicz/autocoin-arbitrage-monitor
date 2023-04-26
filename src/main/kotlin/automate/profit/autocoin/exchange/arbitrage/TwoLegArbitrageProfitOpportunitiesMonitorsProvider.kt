package automate.profit.autocoin.exchange.arbitrage

import automate.profit.autocoin.app.config.ExchangePair
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageOpportunitiesMonitor
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair

class TwoLegArbitrageProfitOpportunitiesMonitorsProvider(
    private val profitCache: TwoLegArbitrageProfitOpportunityCache,
    private val profitCalculator: TwoLegArbitrageProfitOpportunityCalculator,
) {
    fun getTwoLegArbitrageOpportunitiesMonitors(commonCurrencyPairsAtExchanges: Map<CurrencyPair, Set<ExchangePair>>): List<TwoLegArbitrageOpportunitiesMonitor> {
        return commonCurrencyPairsAtExchanges.flatMap {
            it.value.map { exchangePair ->
                TwoLegArbitrageOpportunitiesMonitor(
                    currencyPairWithExchangePair = CurrencyPairWithExchangePair(it.key, exchangePair),
                    profitCache = profitCache,
                    profitCalculator = profitCalculator,
                )
            }
        }
    }
}
