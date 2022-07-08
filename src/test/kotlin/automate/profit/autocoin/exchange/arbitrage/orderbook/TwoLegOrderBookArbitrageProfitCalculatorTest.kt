package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.PriceService
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.order.ExchangeOrderType
import automate.profit.autocoin.exchange.orderbook.OrderBook
import automate.profit.autocoin.exchange.orderbook.OrderBookExchangeOrder
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerFetcher
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit

class TwoLegOrderBookArbitrageProfitCalculatorTest {
    private val currencyPair = CurrencyPair.of("X/Y")
    private val exchangeA = BITTREX
    private val exchangeB = BINANCE
    private val exchangePair = ExchangePair(exchangeA, exchangeB)
    private val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
    private val usdValueFromPriceService = BigDecimal("2.0")
    private val pricesService = mock<PriceService>().apply {
        whenever(getUsdValue(eq("Y"), any())).thenReturn(usdValueFromPriceService)
        whenever(getUsdPrice(eq("Y"))).thenReturn(BigDecimal(0.3))
    }
    private val bigDecimalWhichDoesNotMatter = BigDecimal.ONE
    private val counterCurrency24hVolume = BigDecimal("10.0")
    private val tickerFetcher = mock<TickerFetcher>().apply {
        whenever(getCachedOrFetchTicker(exchangeA, currencyPair)).thenReturn(Ticker(
                currencyPair = currencyPair,
                ask = bigDecimalWhichDoesNotMatter,
                bid = bigDecimalWhichDoesNotMatter,
                baseCurrency24hVolume = bigDecimalWhichDoesNotMatter,
                counterCurrency24hVolume = counterCurrency24hVolume,
                timestamp = Instant.now()
        ))
        whenever(getCachedOrFetchTicker(exchangeB, currencyPair)).thenReturn(Ticker(
                currencyPair = currencyPair,
                ask = bigDecimalWhichDoesNotMatter,
                bid = bigDecimalWhichDoesNotMatter,
                baseCurrency24hVolume = bigDecimalWhichDoesNotMatter,
                counterCurrency24hVolume = BigDecimal("10.0"),
                timestamp = Instant.now()
        ))
    }
    private val orderBookUsdAmountThresholds = listOf(BigDecimal("100.0"), BigDecimal("500.0"))
    private val twoLegArbitrageProfitCalculator = TwoLegOrderBookArbitrageProfitCalculator(pricesService, tickerFetcher, orderBookUsdAmountThresholds)
    private val buyOrderExchangeA = OrderBookExchangeOrder(
            exchangeName = "exchangeA",
            type = ExchangeOrderType.BID_BUY,
            orderedAmount = 500.toBigDecimal(),
            price = 7200.toBigDecimal(),
            currencyPair = currencyPair,
            timestamp = Instant.now()
    )
    private val sellOrderExchangeA = buyOrderExchangeA.copy(type = ExchangeOrderType.ASK_SELL)
    private val buyOrderExchangeB = buyOrderExchangeA.copy(
            exchangeName = "exchangeB"
    )
    private val sellOrderExchangeB = buyOrderExchangeB.copy(type = ExchangeOrderType.ASK_SELL)
    private val orderListDoesNotMatter: List<OrderBookExchangeOrder> = listOf(buyOrderExchangeA)

    @Test
    fun shouldFindNoProfitWhenOrderTooOld() {
        // given
        val tooOldTimestamp = Instant.ofEpochMilli(Instant.now().toEpochMilli() - Duration.of(121, ChronoUnit.MINUTES).toMillis())
        val orderBookPairWithTooOldOrders = OrderBookPair(
                first = OrderBook(
                        buyOrders = listOf(buyOrderExchangeA.copy(timestamp = tooOldTimestamp)),
                        sellOrders = listOf(buyOrderExchangeA)
                ),
                second = OrderBook(
                        buyOrders = listOf(buyOrderExchangeB),
                        sellOrders = listOf(buyOrderExchangeB)
                )
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPairWithTooOldOrders)
        // then
        assertThat(profit).isNull()
    }


    @Test
    fun shouldFindNoProfitWhenSpreadTooSmall() {
        // given
        val orderBookPair = OrderBookPair(
                first = OrderBook(
                        buyOrders = listOf(buyOrderExchangeA),
                        sellOrders = listOf(buyOrderExchangeA)
                ),
                second = OrderBook(
                        buyOrders = listOf(buyOrderExchangeB),
                        sellOrders = listOf(buyOrderExchangeB)
                )
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair)
        // then
        assertThat(profit).isNull()
    }

    @Test
    fun shouldSellAtExchangeA() {

    }

    @Test
    fun shouldSellAtExchangeB() {
        // given
        // sell higher at exchange A, buy lower at exchange B which means
        // buyPrice(A) > sellPrice(B)
        val orderBookPair = OrderBookPair(
                first = OrderBook(
                        buyOrders = listOf(buyOrderExchangeA.copy(
                                price = BigDecimal("7200"),
                                orderedAmount = BigDecimal("400")
                        )),
                        sellOrders = orderListDoesNotMatter
                ),
                second = OrderBook(
                        buyOrders = orderListDoesNotMatter,
                        sellOrders = listOf(sellOrderExchangeB.copy(
                                price = BigDecimal("7150"),
                                orderedAmount = BigDecimal("400")
                        ))
                )
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair)
        // then
        assertThat(profit).isNotNull
        with(profit!!) {
            assertThat(currencyPairWithExchangePair).isEqualTo(currencyPairWithExchangePair)
            assertThat(usd24hVolumeAtFirstExchange).isEqualTo(usdValueFromPriceService)
            assertThat(usd24hVolumeAtSecondExchange).isEqualTo(usdValueFromPriceService)
            assertThat(orderBookArbitrageProfitHistogram).hasSize(orderBookUsdAmountThresholds.size)
            assertThat(orderBookArbitrageProfitHistogram[0]?.sellPrice).isEqualTo(BigDecimal("7200.00000000"))
            assertThat(orderBookArbitrageProfitHistogram[0]?.sellAtExchange).isEqualTo(exchangeA)
            assertThat(orderBookArbitrageProfitHistogram[0]?.buyPrice).isEqualTo(BigDecimal("7150.00000000"))
            assertThat(orderBookArbitrageProfitHistogram[0]?.buyAtExchange).isEqualTo(exchangeB)
            assertThat(orderBookArbitrageProfitHistogram[0]?.relativeProfit).isEqualTo(BigDecimal("0.00699301"))
            assertThat(orderBookArbitrageProfitHistogram[0]?.usdDepthUpTo).isEqualTo(BigDecimal("100.0"))
            assertThat(orderBookArbitrageProfitHistogram[1]).isNotNull
            assertThat(orderBookArbitrageProfitHistogram[1]!!.usdDepthUpTo).isEqualTo(BigDecimal("500.0"))
        }
    }

}