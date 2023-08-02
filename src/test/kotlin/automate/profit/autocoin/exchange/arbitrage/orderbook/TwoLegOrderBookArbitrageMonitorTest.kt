package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.TestExchange.exchangeA
import automate.profit.autocoin.TestExchange.exchangeB
import automate.profit.autocoin.app.config.ExchangePair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.TickerPair
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.orderbook.OrderBook
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever


class TwoLegOrderBookArbitrageMonitorTest {
    private val firstExchange = exchangeA
    private val secondExchange = exchangeB

    @Test
    fun shouldCacheOpportunityWhenOrderBooksAndTickersAvailable() {
        // given
        val currencyPair = CurrencyPair.of("A/B")
        val exchangePair = ExchangePair(firstExchange, secondExchange)
        val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
        val firstOrderBook = mock<OrderBook>()
        val secondOrderBook = mock<OrderBook>()
        val orderBookPair = OrderBookPair(first = firstOrderBook, second = secondOrderBook)
        val tickerPair = TickerPair(first = mock(), second = mock())
        val profitCache = mock<TwoLegArbitrageProfitOpportunityCache>()
        val profit = mock<TwoLegArbitrageProfitOpportunity>()
        val profitCalculator = mock<TwoLegArbitrageProfitOpportunityCalculator>().apply {
            whenever(this.calculateProfit(currencyPairWithExchangePair, orderBookPair, tickerPair)).thenReturn(profit)
        }

        val twoLegArbitrageMonitor = TwoLegArbitrageOpportunitiesMonitor(currencyPairWithExchangePair, profitCache, profitCalculator)
        val orderBookListeners = twoLegArbitrageMonitor.getOrderBookListeners()
        val tickerListeners = twoLegArbitrageMonitor.getTickerListeners()

        // when
        orderBookListeners.first.onOrderBook(firstExchange, currencyPair, firstOrderBook)
        tickerListeners.first.onTicker(firstExchange, currencyPair, tickerPair.first!!)
        orderBookListeners.second.onOrderBook(secondExchange, currencyPair, secondOrderBook)
        tickerListeners.second.onTicker(secondExchange, currencyPair, tickerPair.second!!)
        // then
        verify(profitCache).setProfitOpportunity(profit)
    }

    @Test
    fun shouldCacheOpportunityWhenOnlyOrderBooksAvailable() {
        // given
        val currencyPair = CurrencyPair.of("A/B")
        val exchangePair = ExchangePair(firstExchange, secondExchange)
        val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
        val firstOrderBook = mock<OrderBook>()
        val secondOrderBook = mock<OrderBook>()
        val orderBookPair = OrderBookPair(first = firstOrderBook, second = secondOrderBook)
        val tickerPair = TickerPair(first = null, second = null)
        val profitCache = mock<TwoLegArbitrageProfitOpportunityCache>()
        val profit = mock<TwoLegArbitrageProfitOpportunity>()
        val profitCalculator = mock<TwoLegArbitrageProfitOpportunityCalculator>().apply {
            whenever(this.calculateProfit(currencyPairWithExchangePair, orderBookPair, tickerPair)).thenReturn(profit)
        }

        val twoLegArbitrageMonitor = TwoLegArbitrageOpportunitiesMonitor(currencyPairWithExchangePair, profitCache, profitCalculator)
        val orderBookListeners = twoLegArbitrageMonitor.getOrderBookListeners()

        // when
        orderBookListeners.first.onOrderBook(firstExchange, currencyPair, firstOrderBook)
        orderBookListeners.second.onOrderBook(secondExchange, currencyPair, secondOrderBook)
        // then
        verify(profitCache).setProfitOpportunity(profit)
    }
}
