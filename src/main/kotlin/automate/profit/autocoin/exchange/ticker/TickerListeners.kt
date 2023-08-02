package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.exchange.ExchangeWithCurrencyPair
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageOpportunitiesMonitor
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.ticker.listener.TickerListener

class TickerListeners {
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
            addTickerListener(firstExchangeWithCurrencyPair, tickerListenersPair.first)
            addTickerListener(secondExchangeWithCurrencyPair, tickerListenersPair.second)
        }
    }

    fun addTickerListener(exchangeWithCurrencyPair: ExchangeWithCurrencyPair, listener: TickerListener) {
        tickerListenersCache.computeIfAbsent(exchangeWithCurrencyPair) { ArrayList() }.add(listener)
    }

    fun getTickerListeners(exchange: Exchange, currencyPair: CurrencyPair): List<TickerListener> {
        return tickerListenersCache.getValue(ExchangeWithCurrencyPair(exchange = exchange, currencyPair = currencyPair))
    }

}
