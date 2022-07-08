package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageMonitor
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCalculator
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.metrics.MetricsService

private data class ExchangeWithCurrencyPair(
        val exchange: SupportedExchange,
        val currencyPair: CurrencyPair
)

class OrderBookListenersProvider(
        private val profitCache: TwoLegOrderBookArbitrageProfitCache,
        private val profitCalculator: TwoLegOrderBookArbitrageProfitCalculator,
        private val metricsService: MetricsService
) {
    private val orderBookListenersCache = HashMap<ExchangeWithCurrencyPair, MutableList<OrderBookListener>>()

    fun prepareOrderBookListeners(commonCurrencyPairsAtExchanges: Map<CurrencyPair, Set<ExchangePair>>) {
        commonCurrencyPairsAtExchanges.forEach {
            it.value.forEach { exchangePair ->
                val monitor = TwoLegOrderBookArbitrageMonitor(
                        currencyPairWithExchangePair = CurrencyPairWithExchangePair(it.key, exchangePair),
                        profitCache = profitCache,
                        profitCalculator = profitCalculator,
                        metricsService = metricsService
                )
                val orderBookListenersPair = monitor.getOrderBookListeners()
                val firstExchangeWithCurrencyPair = ExchangeWithCurrencyPair(
                        exchange = exchangePair.firstExchange,
                        currencyPair = it.key
                )
                val secondExchangeWithCurrencyPair = ExchangeWithCurrencyPair(
                        exchange = exchangePair.secondExchange,
                        currencyPair = it.key
                )
                orderBookListenersCache.computeIfAbsent(firstExchangeWithCurrencyPair) { ArrayList() }
                orderBookListenersCache.computeIfAbsent(secondExchangeWithCurrencyPair) { ArrayList() }
                orderBookListenersCache.getValue(firstExchangeWithCurrencyPair).add(orderBookListenersPair.first)
                orderBookListenersCache.getValue(secondExchangeWithCurrencyPair).add(orderBookListenersPair.second)
            }
        }
    }

    fun getOrderBookListeners(exchange: SupportedExchange, currencyPair: CurrencyPair): List<OrderBookListener> {
        return orderBookListenersCache.getValue(ExchangeWithCurrencyPair(exchange = exchange, currencyPair = currencyPair))
    }

}