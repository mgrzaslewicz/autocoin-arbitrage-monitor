package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.orderbook.OrderBookListenersProvider
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class OrderBookListenersProviderTest {
    private val grinUsdt = CurrencyPair.Companion.of("GRIN/USD")
    private val commonCurrencyPairsAtExchanges = mapOf(
            grinUsdt to setOf(
                    ExchangePair(firstExchange = BIBOX, secondExchange = BITTREX),
                    ExchangePair(firstExchange = BIBOX, secondExchange = GATEIO),
                    ExchangePair(firstExchange = BIBOX, secondExchange = KUCOIN),
                    ExchangePair(firstExchange = BIBOX, secondExchange = POLONIEX),
                    ExchangePair(firstExchange = BITTREX, secondExchange = GATEIO),
                    ExchangePair(firstExchange = BITTREX, secondExchange = KUCOIN),
                    ExchangePair(firstExchange = BITTREX, secondExchange = POLONIEX),
                    ExchangePair(firstExchange = GATEIO, secondExchange = KUCOIN),
                    ExchangePair(firstExchange = GATEIO, secondExchange = POLONIEX),
                    ExchangePair(firstExchange = KUCOIN, secondExchange = POLONIEX)
            )
    )

    @Test
    fun shouldProduceListeners() {
        // given
        val tested = OrderBookListenersProvider(
                profitCache = mock(),
                profitCalculator = mock(),
                metricsService = mock()
        )
        tested.prepareOrderBookListeners(commonCurrencyPairsAtExchanges)
        // when-then
        assertThat(tested.getOrderBookListeners(BIBOX, grinUsdt)).hasSize(4)
        assertThat(tested.getOrderBookListeners(BITTREX, grinUsdt)).hasSize(4)
        assertThat(tested.getOrderBookListeners(GATEIO, grinUsdt)).hasSize(4)
        assertThat(tested.getOrderBookListeners(KUCOIN, grinUsdt)).hasSize(4)
        assertThat(tested.getOrderBookListeners(POLONIEX, grinUsdt)).hasSize(4)
    }
}