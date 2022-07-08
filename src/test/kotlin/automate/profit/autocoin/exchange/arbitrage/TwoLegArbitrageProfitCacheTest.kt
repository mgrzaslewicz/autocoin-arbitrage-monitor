package automate.profit.autocoin.exchange.arbitrage

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.util.*

class TwoLegArbitrageProfitCacheTest {
    private val currencyPair1 = CurrencyPair.of("A/B")
    private val currencyPair2 = CurrencyPair.of("C/D")
    private val exchangePair1 = ExchangePair(BITTREX, BINANCE)
    private val exchangePair2 = ExchangePair(KUCOIN, BINANCE)
    private val currencyPairWithExchangePair1 = CurrencyPairWithExchangePair(currencyPair1, exchangePair1)
    private val currencyPairWithExchangePair2 = CurrencyPairWithExchangePair(currencyPair2, exchangePair2)
    private val doesNotMatter = BigDecimal.ONE
    private val exchangeDoesNotMatter = BINANCE
    private val firstProfit = TwoLegArbitrageProfit(
            currencyPair = currencyPair1,
            exchangePair = exchangePair1,
            sellPrice = doesNotMatter,
            buyPrice = doesNotMatter,
            sellAtExchange = exchangeDoesNotMatter,
            buyAtExchange = exchangeDoesNotMatter,
            relativeProfit = doesNotMatter,
            calculatedAtMillis = 1
    )

    @Test
    fun shouldRemoveTooOldProfits() {
        // given
        val timeMillis = ArrayDeque<Long>(listOf(3L, 7L, 9L))
        val profitsCache = TwoLegArbitrageProfitCache(ageOfOldestTwoLegArbitrageProfitToKeepMs = 5) { timeMillis.poll() }
        profitsCache.addProfit(firstProfit.copy(calculatedAtMillis = 1, currencyPair = currencyPair1, exchangePair = exchangePair1))
        profitsCache.addProfit(firstProfit.copy(calculatedAtMillis = 3, currencyPair = currencyPair2, exchangePair = exchangePair2))
        // when-then
        profitsCache.removeTooOldProfits()
        assertThat(profitsCache.getCurrencyPairWithExchangePairs()).containsOnly(currencyPairWithExchangePair1, currencyPairWithExchangePair2)
        // when-then
        profitsCache.removeTooOldProfits()
        assertThat(profitsCache.getCurrencyPairWithExchangePairs()).containsOnly(currencyPairWithExchangePair2)
        // when-then
        profitsCache.removeTooOldProfits()
        assertThat(profitsCache.getCurrencyPairWithExchangePairs()).isEmpty()
    }

}