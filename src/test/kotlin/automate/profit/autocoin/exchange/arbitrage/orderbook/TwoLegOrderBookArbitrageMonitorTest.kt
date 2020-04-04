package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.orderbook.OrderBook
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.TickerPair
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test


class TwoLegOrderBookArbitrageMonitorTest {
    private val firstExchange = BITTREX
    private val secondExchange = BINANCE

    @Test
    fun shouldAddTickerSpreadToCache() {
        // given
        val currencyPair = CurrencyPair.of("A/B")
        val exchangePair = ExchangePair(firstExchange, secondExchange)
        val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
        val firstOrderBook = mock<OrderBook>()
        val secondOrderBook = mock<OrderBook>()
        val orderBoookPair = OrderBookPair(first = firstOrderBook, second = secondOrderBook)
        val tickerPair = TickerPair(first = mock(), second = mock())
        val profitCache = mock<TwoLegOrderBookArbitrageProfitCache>()
        val profit = mock<TwoLegOrderBookArbitrageProfit>()
        val profitCalculator = mock<TwoLegOrderBookArbitrageProfitCalculator>().apply {
            whenever(this.calculateProfit(currencyPairWithExchangePair, orderBoookPair, tickerPair)).thenReturn(profit)
        }

        val twoLegArbitrageMonitor = TwoLegOrderBookArbitrageMonitor(currencyPairWithExchangePair, profitCache, profitCalculator, mock())
        val orderBookListeners = twoLegArbitrageMonitor.getOrderBookListeners()
        val tickerListeners = twoLegArbitrageMonitor.getTickerListeners()

        // when
        orderBookListeners.first.onOrderBook(firstExchange, currencyPair, firstOrderBook)
        tickerListeners.first.onTicker(firstExchange, currencyPair, tickerPair.first)
        orderBookListeners.second.onOrderBook(secondExchange, currencyPair, secondOrderBook)
        tickerListeners.second.onTicker(secondExchange, currencyPair, tickerPair.second)
        // then
        verify(profitCache).setProfit(profit)
    }

}