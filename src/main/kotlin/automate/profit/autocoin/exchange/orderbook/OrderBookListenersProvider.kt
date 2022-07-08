package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.exchange.ExchangeWithCurrencyPair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageMonitor
import automate.profit.autocoin.exchange.currency.CurrencyPair

class OrderBookListenersProvider {
    private val orderBookListenersCache = HashMap<ExchangeWithCurrencyPair, MutableList<OrderBookListener>>()

    fun prepareOrderBookListeners(twoLegOrderBookArbitrageMonitors: List<TwoLegOrderBookArbitrageMonitor>) {
        twoLegOrderBookArbitrageMonitors.forEach { monitor ->
            val orderBookListenersPair = monitor.getOrderBookListeners()
            val firstExchangeWithCurrencyPair = ExchangeWithCurrencyPair(
                    exchange = monitor.currencyPairWithExchangePair.exchangePair.firstExchange,
                    currencyPair = monitor.currencyPairWithExchangePair.currencyPair
            )
            val secondExchangeWithCurrencyPair = ExchangeWithCurrencyPair(
                    exchange = monitor.currencyPairWithExchangePair.exchangePair.secondExchange,
                    currencyPair = monitor.currencyPairWithExchangePair.currencyPair
            )
            orderBookListenersCache.computeIfAbsent(firstExchangeWithCurrencyPair) { ArrayList() }
            orderBookListenersCache.computeIfAbsent(secondExchangeWithCurrencyPair) { ArrayList() }
            orderBookListenersCache.getValue(firstExchangeWithCurrencyPair).add(orderBookListenersPair.first)
            orderBookListenersCache.getValue(secondExchangeWithCurrencyPair).add(orderBookListenersPair.second)
        }
    }

    fun getOrderBookListeners(exchange: SupportedExchange, currencyPair: CurrencyPair): List<OrderBookListener> {
        return orderBookListenersCache.getValue(ExchangeWithCurrencyPair(exchange = exchange, currencyPair = currencyPair))
    }

}