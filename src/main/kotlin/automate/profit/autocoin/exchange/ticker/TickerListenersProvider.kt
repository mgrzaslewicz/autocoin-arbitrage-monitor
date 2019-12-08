package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.arbitrage.ticker.TwoLegTickerArbitrageMonitor
import automate.profit.autocoin.exchange.arbitrage.ticker.TwoLegTickerArbitrageProfitCache
import automate.profit.autocoin.exchange.arbitrage.ticker.TwoLegTickerArbitrageProfitCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair

class TickerListenersProvider(
        private val tickerPairCache: TickerPairCache,
        private val twoLegTickerArbitrageProfitCalculator: TwoLegTickerArbitrageProfitCalculator,
        private val twoLegTickerArbitrageProfitCache: TwoLegTickerArbitrageProfitCache
) {
    fun createTickerListenersFrom(commonCurrencyPairsAtExchanges: Map<CurrencyPair, List<ExchangePair>>): List<TickerListener> {
        return commonCurrencyPairsAtExchanges.flatMap {
            it.value.map { exchangePair -> TwoLegTickerArbitrageMonitor(CurrencyPairWithExchangePair(it.key, exchangePair), tickerPairCache, twoLegTickerArbitrageProfitCalculator, twoLegTickerArbitrageProfitCache) }
        }.flatMap {
            it.getTickerListeners().toList()
        }
    }
}