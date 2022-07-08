package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.orderbook.OrderBook
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import org.junit.jupiter.api.Test
import java.util.concurrent.Executors


class TwoLegOrderBookArbitrageMonitorTest {

    @Test
    fun shouldAddTickerSpreadToCache() {
        // given
        val currencyPair = CurrencyPair.of("TESTA/TESTB")
        val exchangePair = ExchangePair(BITTREX, BINANCE)
        val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
        val firstOrderBook = mock<OrderBook>()
        val secondOrderBook = mock<OrderBook>()
        val orderBoookPair = OrderBookPair(first = firstOrderBook, second = secondOrderBook)
        val profitCache = mock<TwoLegOrderBookArbitrageProfitCache>()
        val profit = mock<TwoLegOrderBookArbitrageProfit>()
        val profitCalculator = mock<TwoLegOrderBookArbitrageProfitCalculator>().apply {
            whenever(this.calculateProfit(currencyPairWithExchangePair, orderBoookPair)).thenReturn(profit)
        }

        val twoLegArbitrageMonitor = TwoLegOrderBookArbitrageMonitor(currencyPairWithExchangePair, profitCache, profitCalculator, mock(), mock(), Executors.newSingleThreadExecutor())
        val listeners = twoLegArbitrageMonitor.getOrderBookListeners()

        // when
        listeners.first.onOrderBook(firstOrderBook)
        listeners.second.onOrderBook(secondOrderBook)
        // then
        verify(profitCache).setProfit(profit)
    }

}