package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.app.config.ExchangePair
import automate.profit.autocoin.exchange.RestPriceService
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal
import java.time.Instant

class TwoLegOrderBookArbitrageProfitStatisticsCalculatorTest {

    private val currencyPair = CurrencyPair.of("A/B")
    private val exchangePair = ExchangePair(SupportedExchange.BITTREX, SupportedExchange.BINANCE)
    private val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
    private val usd24hVolume = BigDecimal(2000.0)
    private val pricesService = mock<RestPriceService>().apply {
        whenever(getUsdValue(eq("B"), any())).thenReturn(usd24hVolume)
    }
    private val currentFixedTimeMs = 10L
    private val freshTickerTime = Instant.ofEpochMilli(15L)

    @Test
    // TODO
    fun shouldCalculateStatistics() {

    }

}
