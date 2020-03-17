package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.arbitrage.orderbook.FileOrderBookArbitrageProfitRepository
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageMonitor
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.metrics.MetricsService
import java.util.concurrent.ExecutorService

class OrderBookListenersProvider(
        private val profitCache: TwoLegOrderBookArbitrageProfitCache,
        private val profitCalculator: TwoLegOrderBookArbitrageProfitCalculator,
        private val metricsService: MetricsService,
        private val arbitrageProfitRepository: FileOrderBookArbitrageProfitRepository,
        private val executorService: ExecutorService
) {
    fun createOrderBookListenersFrom(commonCurrencyPairsAtExchanges: Map<CurrencyPair, Set<ExchangePair>>): List<OrderBookListener> {
        return commonCurrencyPairsAtExchanges.flatMap {
            it.value.map { exchangePair ->
                TwoLegOrderBookArbitrageMonitor(
                        currencyPairWithExchangePair = CurrencyPairWithExchangePair(it.key, exchangePair),
                        profitCache = profitCache,
                        profitCalculator = profitCalculator,
                        metricsService = metricsService,
                        arbitrageProfitRepository = arbitrageProfitRepository,
                        executorService = executorService
                )
            }
        }.flatMap {
            it.getOrderBookListeners().toList()
        }
    }
}