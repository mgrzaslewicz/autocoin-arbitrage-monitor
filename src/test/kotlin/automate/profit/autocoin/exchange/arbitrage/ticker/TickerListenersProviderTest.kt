package automate.profit.autocoin.exchange.arbitrage.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.arbitrage.TwoLegOrderBookArbitrageMonitorProvider
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.TickerListenersProvider
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class TickerListenersProviderTest {
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
    private val twoLegOrderBookArbitrageMonitorProvider = TwoLegOrderBookArbitrageMonitorProvider(
            profitCache = mock(),
            profitCalculator = mock(),
            metricsService = mock()
    )

    @Test
    fun shouldPrepareListeners() {
        // given
        val tested = TickerListenersProvider()
        tested.prepareTickerListeners(twoLegOrderBookArbitrageMonitorProvider.getTwoLegOrderBookArbitrageMonitors(commonCurrencyPairsAtExchanges))
        // when-then
        assertThat(tested.getTickerListeners(BIBOX, grinUsdt)).hasSize(4)
        assertThat(tested.getTickerListeners(BITTREX, grinUsdt)).hasSize(4)
        assertThat(tested.getTickerListeners(GATEIO, grinUsdt)).hasSize(4)
        assertThat(tested.getTickerListeners(KUCOIN, grinUsdt)).hasSize(4)
        assertThat(tested.getTickerListeners(POLONIEX, grinUsdt)).hasSize(4)
    }
}