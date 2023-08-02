package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.TestExchange.exchangeA
import automate.profit.autocoin.TestExchange.exchangeB
import automate.profit.autocoin.TestExchange.exchangeC
import automate.profit.autocoin.TestExchange.exchangeD
import automate.profit.autocoin.TestExchange.exchangeE
import automate.profit.autocoin.app.config.ExchangePair
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitOpportunitiesMonitorsProvider
import automate.profit.autocoin.exchange.orderbook.OrderBookListeners
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock

class OrderBookListenersProviderTest {
    private val grinUsdt = CurrencyPair.Companion.of("GRIN/USD")
    private val commonCurrencyPairsAtExchanges = mapOf(
        grinUsdt to setOf(
            ExchangePair(firstExchange = exchangeA, secondExchange = exchangeB),
            ExchangePair(firstExchange = exchangeA, secondExchange = exchangeC),
            ExchangePair(firstExchange = exchangeA, secondExchange = exchangeD),
            ExchangePair(firstExchange = exchangeA, secondExchange = exchangeE),
            ExchangePair(firstExchange = exchangeB, secondExchange = exchangeC),
            ExchangePair(firstExchange = exchangeB, secondExchange = exchangeD),
            ExchangePair(firstExchange = exchangeB, secondExchange = exchangeE),
            ExchangePair(firstExchange = exchangeC, secondExchange = exchangeD),
            ExchangePair(firstExchange = exchangeC, secondExchange = exchangeE),
            ExchangePair(firstExchange = exchangeD, secondExchange = exchangeE)
        )
    )
    private val twoLegArbitrageProfitOpportunitiesMonitorsProvider = TwoLegArbitrageProfitOpportunitiesMonitorsProvider(
        profitCache = mock(),
        profitCalculator = mock(),
    )

    @Test
    fun shouldPrepareListeners() {
        // given
        val tested = OrderBookListeners()
        tested.prepareOrderBookListeners(
            twoLegArbitrageProfitOpportunitiesMonitorsProvider.getTwoLegArbitrageOpportunitiesMonitors(
                commonCurrencyPairsAtExchanges
            )
        )
        // when-then
        assertThat(tested.getOrderBookListeners(exchangeA, grinUsdt)).hasSize(4)
        assertThat(tested.getOrderBookListeners(exchangeB, grinUsdt)).hasSize(4)
        assertThat(tested.getOrderBookListeners(exchangeC, grinUsdt)).hasSize(4)
        assertThat(tested.getOrderBookListeners(exchangeD, grinUsdt)).hasSize(4)
        assertThat(tested.getOrderBookListeners(exchangeE, grinUsdt)).hasSize(4)
    }
}
