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
    private val orderBookListenersCache = HashMap<ExchangeWithCurrencyPair, OrderBookListener>()

    fun prepareOrderBookListeners(commonCurrencyPairsAtExchanges: Map<CurrencyPair, Set<ExchangePair>>) {
        commonCurrencyPairsAtExchanges.forEach {
            it.value.flatMap { exchangePair ->
                val monitor = TwoLegOrderBookArbitrageMonitor(
                        currencyPairWithExchangePair = CurrencyPairWithExchangePair(it.key, exchangePair),
                        profitCache = profitCache,
                        profitCalculator = profitCalculator,
                        metricsService = metricsService
                )
                val orderBookListenersPair = monitor.getOrderBookListeners()
                orderBookListenersCache[ExchangeWithCurrencyPair(
                        exchange = exchangePair.firstExchange,
                        currencyPair = it.key
                )] = orderBookListenersPair.first
                orderBookListenersCache[ExchangeWithCurrencyPair(
                        exchange = exchangePair.secondExchange,
                        currencyPair = it.key
                )] = orderBookListenersPair.second
                orderBookListenersPair.toList()
            }
        }
    }

    fun getOrderBookListener(exchange: SupportedExchange, currencyPair: CurrencyPair): OrderBookListener {
        return orderBookListenersCache.getValue(ExchangeWithCurrencyPair(exchange = exchange, currencyPair = currencyPair))
    }

}