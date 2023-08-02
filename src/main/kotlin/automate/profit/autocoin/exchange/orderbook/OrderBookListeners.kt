package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.exchange.ExchangeWithCurrencyPair
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageOpportunitiesMonitor
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.orderbook.listener.OrderBookListener

class OrderBookListeners {
    private val orderBookListenersCache = HashMap<ExchangeWithCurrencyPair, MutableList<OrderBookListener>>()

    fun prepareOrderBookListeners(twoLegArbitrageOpportunitiesMonitors: List<TwoLegArbitrageOpportunitiesMonitor>) {
        twoLegArbitrageOpportunitiesMonitors.forEach { monitor ->
            val orderBookListenersPair = monitor.getOrderBookListeners()
            val firstExchangeWithCurrencyPair = ExchangeWithCurrencyPair(
                exchange = monitor.currencyPairWithExchangePair.exchangePair.firstExchange,
                currencyPair = monitor.currencyPairWithExchangePair.currencyPair
            )
            val secondExchangeWithCurrencyPair = ExchangeWithCurrencyPair(
                exchange = monitor.currencyPairWithExchangePair.exchangePair.secondExchange,
                currencyPair = monitor.currencyPairWithExchangePair.currencyPair
            )
            addOrderBookListener(firstExchangeWithCurrencyPair, orderBookListenersPair.first)
            addOrderBookListener(secondExchangeWithCurrencyPair, orderBookListenersPair.second)
        }
    }

    fun addOrderBookListener(exchangeWithCurrencyPair: ExchangeWithCurrencyPair, listener: OrderBookListener) {
        orderBookListenersCache.computeIfAbsent(exchangeWithCurrencyPair) { ArrayList() }.add(listener)
    }

    fun getOrderBookListeners(exchange: Exchange, currencyPair: CurrencyPair): List<OrderBookListener> {
        return orderBookListenersCache.getValue(ExchangeWithCurrencyPair(exchange = exchange, currencyPair = currencyPair))
    }

}
