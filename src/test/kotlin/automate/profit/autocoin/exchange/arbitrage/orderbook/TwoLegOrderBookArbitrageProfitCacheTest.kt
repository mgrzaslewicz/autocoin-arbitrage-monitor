package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageRelativeProfitGroup.INACCURATE_NOT_USING_METADATA
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class TwoLegOrderBookArbitrageProfitCacheTest {
    private val currencyPair1 = CurrencyPair.of("A/B")
    private val currencyPair2 = CurrencyPair.of("C/D")
    private val exchangePair1 = ExchangePair(BITTREX, BINANCE)
    private val exchangePair2 = ExchangePair(KUCOIN, BINANCE)
    private val currencyPairWithExchangePair1 = CurrencyPairWithExchangePair(currencyPair1, exchangePair1)
    private val currencyPairWithExchangePair2 = CurrencyPairWithExchangePair(currencyPair2, exchangePair2)

    private val sampleProfit = TwoLegOrderBookArbitrageProfit(
        currencyPairWithExchangePair = currencyPairWithExchangePair1,
        usd24hVolumeAtFirstExchange = 1000.0.toBigDecimal(),
        usd24hVolumeAtSecondExchange = 1500.0.toBigDecimal(),
        orderBookArbitrageProfitHistogram = listOf(),
        calculatedAtMillis = 1,
    )

    @Test
    fun shouldRemoveTooOldProfits() {
        // given
        val timeMillis = ArrayDeque(listOf(3L, 7L, 9L))
        val profitsCache = TwoLegOrderBookArbitrageProfitCache(ageOfOldestTwoLegArbitrageProfitToKeepMs = 5) { timeMillis.poll() }
        val cacheGrup = INACCURATE_NOT_USING_METADATA
        profitsCache.setProfit(cacheGrup, sampleProfit.copy(calculatedAtMillis = 1))
        profitsCache.setProfit(cacheGrup, sampleProfit.copy(calculatedAtMillis = 3, currencyPairWithExchangePair = currencyPairWithExchangePair2))
        // when-then
        profitsCache.removeTooOldProfits()
        assertThat(profitsCache.getCurrencyPairWithExchangePairs(cacheGrup)).containsOnly(currencyPairWithExchangePair1, currencyPairWithExchangePair2)
        // when-then
        profitsCache.removeTooOldProfits()
        assertThat(profitsCache.getCurrencyPairWithExchangePairs(cacheGrup)).containsOnly(currencyPairWithExchangePair2)
        // when-then
        profitsCache.removeTooOldProfits()
        assertThat(profitsCache.getCurrencyPairWithExchangePairs(cacheGrup)).isEmpty()
    }

}
