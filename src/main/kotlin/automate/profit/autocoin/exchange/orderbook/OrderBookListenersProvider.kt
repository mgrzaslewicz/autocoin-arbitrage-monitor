package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageMonitor
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair

class OrderBookListenersProvider(
        private val profitCache: TwoLegOrderBookArbitrageProfitCache,
        private val profitCalculator: TwoLegOrderBookArbitrageProfitCalculator
) {
    fun createOrderBookListenersFrom(commonCurrencyPairsAtExchanges: Map<CurrencyPair, List<ExchangePair>>): List<OrderBookListener> {
        return commonCurrencyPairsAtExchanges.flatMap {
            it.value.map { exchangePair ->
                TwoLegOrderBookArbitrageMonitor(
                        CurrencyPairWithExchangePair(it.key, exchangePair),
                        profitCache,
                        profitCalculator
                )
            }
        }.flatMap {
            it.getOrderBookListeners().toList()
        }
    }
}