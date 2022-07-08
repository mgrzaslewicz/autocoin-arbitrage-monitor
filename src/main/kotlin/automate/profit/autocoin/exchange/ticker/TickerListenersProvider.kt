package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.exchange.ExchangeWithCurrencyPair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageOpportunitiesMonitor
import automate.profit.autocoin.exchange.currency.CurrencyPair

class TickerListenersProvider {
    private val tickerListenersCache = HashMap<ExchangeWithCurrencyPair, MutableList<TickerListener>>()

    fun prepareTickerListeners(twoLegArbitrageOpportunitiesMonitors: List<TwoLegArbitrageOpportunitiesMonitor>) {
        twoLegArbitrageOpportunitiesMonitors.forEach { monitor ->
            val tickerListenersPair = monitor.getTickerListeners()
            val firstExchangeWithCurrencyPair = ExchangeWithCurrencyPair(
                    exchange = monitor.currencyPairWithExchangePair.exchangePair.firstExchange,
                    currencyPair = monitor.currencyPairWithExchangePair.currencyPair
            )
            val secondExchangeWithCurrencyPair = ExchangeWithCurrencyPair(
                    exchange = monitor.currencyPairWithExchangePair.exchangePair.secondExchange,
                    currencyPair = monitor.currencyPairWithExchangePair.currencyPair
            )
            tickerListenersCache.computeIfAbsent(firstExchangeWithCurrencyPair) { ArrayList() }
            tickerListenersCache.computeIfAbsent(secondExchangeWithCurrencyPair) { ArrayList() }
            tickerListenersCache.getValue(firstExchangeWithCurrencyPair).add(tickerListenersPair.first)
            tickerListenersCache.getValue(secondExchangeWithCurrencyPair).add(tickerListenersPair.second)
        }
    }


    fun getTickerListeners(exchange: SupportedExchange, currencyPair: CurrencyPair): List<TickerListener> {
        return tickerListenersCache.getValue(ExchangeWithCurrencyPair(exchange = exchange, currencyPair = currencyPair))
    }

}
