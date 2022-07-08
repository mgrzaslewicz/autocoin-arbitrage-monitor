package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageMonitor
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.metrics.MetricsService

interface DeprecatedOrderBookListener: OrderBookListener {
    fun currencyPair(): CurrencyPair
    fun exchange(): SupportedExchange
}

class DeprecatedOrderBookListenersProvider(
        private val profitCache: TwoLegOrderBookArbitrageProfitCache,
        private val profitCalculator: TwoLegOrderBookArbitrageProfitCalculator,
        private val metricsService: MetricsService
) {
    fun createOrderBookListenersFrom(commonCurrencyPairsAtExchanges: Map<CurrencyPair, Set<ExchangePair>>): List<DeprecatedOrderBookListener> {
        return commonCurrencyPairsAtExchanges.flatMap {
            it.value.map { exchangePair ->
                TwoLegOrderBookArbitrageMonitor(
                        currencyPairWithExchangePair = CurrencyPairWithExchangePair(it.key, exchangePair),
                        profitCache = profitCache,
                        profitCalculator = profitCalculator,
                        metricsService = metricsService
                )
            }
        }.flatMap {
            it.getOrderBookListeners().toList()
        }
    }
}