package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.arbitrage.orderbook.FileOrderBookArbitrageProfitRepository
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageMonitor
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import com.timgroup.statsd.StatsDClient

class OrderBookListenersProvider(
        private val profitCache: TwoLegOrderBookArbitrageProfitCache,
        private val profitCalculator: TwoLegOrderBookArbitrageProfitCalculator,
        private val statsDClient: StatsDClient,
        private val arbitrageProfitRepository: FileOrderBookArbitrageProfitRepository
) {
    fun createOrderBookListenersFrom(commonCurrencyPairsAtExchanges: Map<CurrencyPair, List<ExchangePair>>): List<OrderBookListener> {
        return commonCurrencyPairsAtExchanges.flatMap {
            it.value.map { exchangePair ->
                TwoLegOrderBookArbitrageMonitor(
                        currencyPairWithExchangePair = CurrencyPairWithExchangePair(it.key, exchangePair),
                        profitCache = profitCache,
                        profitCalculator = profitCalculator,
                        statsDClient = statsDClient,
                        arbitrageProfitRepository = arbitrageProfitRepository
                )
            }
        }.flatMap {
            it.getOrderBookListeners().toList()
        }
    }
}