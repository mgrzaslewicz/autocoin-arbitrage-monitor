package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageRelativeProfitGroup.INACCURATE_NOT_USING_METADATA
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import com.nhaarman.mockitokotlin2.mock
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.util.*

class TwoLegOrderBookArbitrageProfitCacheTest {
    private val currencyPair1 = CurrencyPair.of("A/B")
    private val currencyPair2 = CurrencyPair.of("C/D")
    private val exchangePair1 = ExchangePair(BITTREX, BINANCE)
    private val exchangePair2 = ExchangePair(KUCOIN, BINANCE)
    private val exchangePair3 = ExchangePair(KUCOIN, BITTREX)
    private val currencyPairWithExchangePair1 = CurrencyPairWithExchangePair(currencyPair1, exchangePair1)
    private val currencyPairWithExchangePair2 = CurrencyPairWithExchangePair(currencyPair2, exchangePair2)
    private val currencyPairWithExchangePair3 = CurrencyPairWithExchangePair(currencyPair2, exchangePair3)
    private val currencyPairWithExchangePair4 = CurrencyPairWithExchangePair(currencyPair1, exchangePair3)

    private val noProfitSample = TwoLegOrderBookArbitrageProfit(
        currencyPairWithExchangePair = currencyPairWithExchangePair1,
        usd24hVolumeAtFirstExchange = 1000.0.toBigDecimal(),
        usd24hVolumeAtSecondExchange = 1500.0.toBigDecimal(),
        orderBookArbitrageProfitHistogram = listOf(),
        calculatedAtMillis = 1,
    )

    private val sampleProfit = noProfitSample.copy(
        orderBookArbitrageProfitHistogram = listOf(
            null,
            mock()
        )
    )

    @Test
    fun shouldRemoveTooOldProfits() {
        // given
        val timeMillis = ArrayDeque(listOf(3L, 7L, 9L))
        val profitsCache = TwoLegOrderBookArbitrageProfitOpportunityCache(ageOfOldestTwoLegArbitrageProfitToKeepMs = 5) { timeMillis.poll() }
        val cacheGroup = INACCURATE_NOT_USING_METADATA
        profitsCache.setProfitOpportunity(cacheGroup, noProfitSample.copy(calculatedAtMillis = 1))
        profitsCache.setProfitOpportunity(cacheGroup, noProfitSample.copy(calculatedAtMillis = 3, currencyPairWithExchangePair = currencyPairWithExchangePair2))
        // when-then
        profitsCache.removeTooOldProfits()
        assertThat(profitsCache.getCurrencyPairWithExchangePairs(cacheGroup)).containsOnly(currencyPairWithExchangePair1, currencyPairWithExchangePair2)
        // when-then
        profitsCache.removeTooOldProfits()
        assertThat(profitsCache.getCurrencyPairWithExchangePairs(cacheGroup)).containsOnly(currencyPairWithExchangePair2)
        // when-then
        profitsCache.removeTooOldProfits()
        assertThat(profitsCache.getCurrencyPairWithExchangePairs(cacheGroup)).isEmpty()
    }

    @Test
    fun shouldGetExchangePairsOpportunityCountWhenNoOpportunities() {
        // given
        val profitsCache = TwoLegOrderBookArbitrageProfitOpportunityCache(ageOfOldestTwoLegArbitrageProfitToKeepMs = 5)
        val cacheGroup = INACCURATE_NOT_USING_METADATA
        profitsCache.setProfitOpportunity(cacheGroup, noProfitSample)
        profitsCache.setProfitOpportunity(cacheGroup, noProfitSample.copy(currencyPairWithExchangePair = currencyPairWithExchangePair2))
        profitsCache.setProfitOpportunity(cacheGroup, noProfitSample.copy(currencyPairWithExchangePair = currencyPairWithExchangePair3))
        profitsCache.setProfitOpportunity(cacheGroup, noProfitSample.copy(currencyPairWithExchangePair = currencyPairWithExchangePair4))
        // when
        val exchangePairsOpportunityCount = profitsCache.getExchangePairsOpportunityCount(cacheGroup)
        // then
        assertThat(exchangePairsOpportunityCount).containsExactlyInAnyOrder(
            ExchangePairWithOpportunityCount(
                exchangePair = exchangePair1,
                opportunityCount = 0,
            ),
            ExchangePairWithOpportunityCount(
                exchangePair = exchangePair2,
                opportunityCount = 0,
            ),
            ExchangePairWithOpportunityCount(
                exchangePair = exchangePair3,
                opportunityCount = 0,
            ),
        )
    }

    @Test
    fun shouldGetExchangePairsOpportunityCount() {
        // given
        val profitsCache = TwoLegOrderBookArbitrageProfitOpportunityCache(ageOfOldestTwoLegArbitrageProfitToKeepMs = 5)
        val cacheGroup = INACCURATE_NOT_USING_METADATA
        profitsCache.setProfitOpportunity(cacheGroup, sampleProfit)
        profitsCache.setProfitOpportunity(cacheGroup, sampleProfit.copy(currencyPairWithExchangePair = currencyPairWithExchangePair2))
        profitsCache.setProfitOpportunity(cacheGroup, sampleProfit.copy(currencyPairWithExchangePair = currencyPairWithExchangePair3))
        profitsCache.setProfitOpportunity(cacheGroup, sampleProfit.copy(currencyPairWithExchangePair = currencyPairWithExchangePair4))
        // when
        val exchangePairsOpportunityCount = profitsCache.getExchangePairsOpportunityCount(cacheGroup)
        // then
        assertThat(exchangePairsOpportunityCount).containsExactlyInAnyOrder(
            ExchangePairWithOpportunityCount(
                exchangePair = exchangePair1,
                opportunityCount = 1,
            ),
            ExchangePairWithOpportunityCount(
                exchangePair = exchangePair2,
                opportunityCount = 1,
            ),
            ExchangePairWithOpportunityCount(
                exchangePair = exchangePair3,
                opportunityCount = 2,
            ),
        )
    }
}
