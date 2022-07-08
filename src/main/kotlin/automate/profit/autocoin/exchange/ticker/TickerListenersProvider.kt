package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageMonitor
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitCache
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair

class TickerListenersProvider(
        private val tickerPairCache: TickerPairCache,
        private val twoLegArbitrageProfitCalculator: TwoLegArbitrageProfitCalculator,
        private val twoLegArbitrageProfitCache: TwoLegArbitrageProfitCache
) {
    fun createTickerListenersFrom(commonCurrencyPairsAtExchanges: Map<CurrencyPair, List<ExchangePair>>): List<TickerListener> {
        return commonCurrencyPairsAtExchanges.flatMap {
            it.value.map { exchangePair -> TwoLegArbitrageMonitor(CurrencyPairWithExchangePair(it.key, exchangePair), tickerPairCache, twoLegArbitrageProfitCalculator, twoLegArbitrageProfitCache) }
        }.flatMap {
            it.getTickerListeners().toList()
        }
    }
}