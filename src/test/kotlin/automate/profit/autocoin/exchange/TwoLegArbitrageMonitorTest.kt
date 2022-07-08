package automate.profit.autocoin.exchange

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageMonitor
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerPair
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
        val tickerPairCache = mock<TickerPairCache>()
        val exchangePair = ExchangePair(BITTREX, BINANCE)
        val volumeDoesNotMatter = BigDecimal.ONE
        val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
        val twoLegArbitrageMonitor = TwoLegArbitrageMonitor(currencyPairWithExchangePair, tickerPairCache, mock(), mock())
        val tickerListeners = twoLegArbitrageMonitor.getTickerListeners()
        val firstExchangeTicker = Ticker(currencyPair = currencyPair, ask = BigDecimal("1.5"), bid = BigDecimal("1.52"), timestamp = null, baseCurrency24hVolume = volumeDoesNotMatter, counterCurrency24hVolume = volumeDoesNotMatter)
        val secondExchangeTicker = Ticker(currencyPair = currencyPair, ask = BigDecimal("1.506"), bid = BigDecimal("1.523"), timestamp = Instant.now(), baseCurrency24hVolume = volumeDoesNotMatter, counterCurrency24hVolume = volumeDoesNotMatter)
        // when
        tickerListeners.first.onTicker(firstExchangeTicker)
        tickerListeners.second.onTicker(secondExchangeTicker)
        // then
        verify(tickerPairCache).addTickerPair(currencyPairWithExchangePair, TickerPair(firstExchangeTicker, secondExchangeTicker))
    }

}