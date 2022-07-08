package automate.profit.autocoin.exchange

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerPairCache
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Instant


class TwoLegArbitrageMonitorTest {

    @Test
    fun shouldAddTickerSpreadToCache() {
        // given
        val currencyPair = CurrencyPair.of("TESTA/TESTB")
        val tickerSpreadCache = mock<TickerPairCache>()
        val exchangePair = ExchangePair(BITTREX, BINANCE)
        val twoLegArbitrageMonitor = TwoLegArbitrageMonitor(currencyPair, exchangePair, tickerSpreadCache)
        val tickerListeners = twoLegArbitrageMonitor.getTickerListeners()
        val firstExchangeTicker = Ticker(currencyPair = currencyPair, ask = BigDecimal("1.5"), bid = BigDecimal("1.52"), timestamp = null)
        val secondExchangeTicker = Ticker(currencyPair = currencyPair, ask = BigDecimal("1.506"), bid = BigDecimal("1.523"), timestamp = Instant.now())
        // when
        tickerListeners.first.onTicker(firstExchangeTicker)
        tickerListeners.second.onTicker(secondExchangeTicker)
        // then
        verify(tickerSpreadCache).addTickerPair(currencyPair, exchangePair, firstExchangeTicker, secondExchangeTicker)
    }

}