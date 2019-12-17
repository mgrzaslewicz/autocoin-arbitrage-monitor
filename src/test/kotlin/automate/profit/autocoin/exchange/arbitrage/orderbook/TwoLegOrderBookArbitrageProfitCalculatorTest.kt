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
        whenever(getCachedTicker(exchangeA, currencyPair)).thenReturn(Ticker(
                currencyPair = currencyPair,
                ask = bigDecimalWhichDoesNotMatter,
                bid = bigDecimalWhichDoesNotMatter,
                baseCurrency24hVolume = bigDecimalWhichDoesNotMatter,
                counterCurrency24hVolume = counterCurrency24hVolume,
                timestamp = Instant.now()
        ))
        whenever(getCachedTicker(exchangeB, currencyPair)).thenReturn(Ticker(
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
    private val sampleOrderExchangeA = OrderBookExchangeOrder(
            exchangeName = "exchangeA",
            type = ExchangeOrderType.BID_BUY,
            orderedAmount = 10.toBigDecimal(),
            price = 1.5.toBigDecimal(),
            currencyPair = currencyPair,
            timestamp = null
    )
    private val sampleOrderExchangeB = sampleOrderExchangeA.copy(
            exchangeName = "exchangeB"
    )

    @Test
    fun shouldFindNoProfitWhenOrderTooOld() {
        // given
        val tooOldTimestamp = Instant.ofEpochMilli(Instant.now().toEpochMilli() - Duration.of(121, ChronoUnit.MINUTES).toMillis())
        val orderBookPairWithTooOldOrders = OrderBookPair(
                first = OrderBook(
                        buyOrders = listOf(sampleOrderExchangeA.copy(timestamp = tooOldTimestamp)),
                        sellOrders = listOf(sampleOrderExchangeA)
                ),
                second = OrderBook(
                        buyOrders = listOf(sampleOrderExchangeB),
                        sellOrders = listOf(sampleOrderExchangeB)
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
                        buyOrders = listOf(sampleOrderExchangeA.copy(timestamp = Instant.now())),
                        sellOrders = listOf(sampleOrderExchangeA.copy(timestamp = Instant.now()))
                ),
                second = OrderBook(
                        buyOrders = listOf(sampleOrderExchangeB.copy(timestamp = Instant.now())),
                        sellOrders = listOf(sampleOrderExchangeB.copy(timestamp = Instant.now()))
                )
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair)
        // then
        assertThat(profit).isNull()
    }

    @Test
    fun shouldSellAtExchangeA() {
        // given
        val orderBookPair = OrderBookPair(
                first = OrderBook(
                        buyOrders = listOf(sampleOrderExchangeA.copy(
                                timestamp = Instant.now(),
                                price = BigDecimal("1.0021"),
                                orderedAmount = BigDecimal("340.0")
                        )),
                        sellOrders = listOf(mock())
                ),
                second = OrderBook(
                        buyOrders = listOf(sampleOrderExchangeA.copy(
                                timestamp = Instant.now(),
                                price = BigDecimal("1.001"),
                                orderedAmount = BigDecimal("340.0")
                        )),
                        sellOrders = listOf(mock())
                )
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair)
        // then
        assertThat(profit).isNotNull
        assertThat(profit!!.currencyPairWithExchangePair).isEqualTo(currencyPairWithExchangePair)
        assertThat(profit.usd24hVolumeAtFirstExchange).isEqualTo(usdValueFromPriceService)
        assertThat(profit.usd24hVolumeAtSecondExchange).isEqualTo(usdValueFromPriceService)
        assertThat(profit.orderBookArbitrageProfitHistogram).hasSize(orderBookUsdAmountThresholds.size)
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.sellPrice).isEqualTo(BigDecimal("1.00210000"))
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.sellAtExchange).isEqualTo(exchangeA)
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.buyPrice).isEqualTo(BigDecimal("1.00100000"))
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.buyAtExchange).isEqualTo(exchangeB)
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.relativeProfit).isEqualTo(BigDecimal("0.00109890"))
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.usdDepthUpTo).isEqualTo(BigDecimal("100.0"))
        assertThat(profit.orderBookArbitrageProfitHistogram[1]).isNull()
    }

    @Test
    fun shouldSellAtExchangeB() {
        // given
        val orderBookPair = OrderBookPair(
                first = OrderBook(
                        buyOrders = listOf(sampleOrderExchangeA.copy(
                                timestamp = Instant.now(),
                                price = BigDecimal("1.001"),
                                orderedAmount = BigDecimal("340.0")
                        )),
                        sellOrders = listOf(mock())
                ),
                second = OrderBook(
                        buyOrders = listOf(sampleOrderExchangeA.copy(
                                timestamp = Instant.now(),
                                price = BigDecimal("1.0021"),
                                orderedAmount = BigDecimal("340.0")
                        )),
                        sellOrders = listOf(mock())
                )
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair)
        // then
        assertThat(profit).isNotNull
        assertThat(profit!!.currencyPairWithExchangePair).isEqualTo(currencyPairWithExchangePair)
        assertThat(profit.usd24hVolumeAtFirstExchange).isEqualTo(usdValueFromPriceService)
        assertThat(profit.usd24hVolumeAtSecondExchange).isEqualTo(usdValueFromPriceService)
        assertThat(profit.orderBookArbitrageProfitHistogram).hasSize(orderBookUsdAmountThresholds.size)
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.sellPrice).isEqualTo(BigDecimal("1.00210000"))
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.sellAtExchange).isEqualTo(exchangeB)
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.buyPrice).isEqualTo(BigDecimal("1.00100000"))
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.buyAtExchange).isEqualTo(exchangeA)
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.relativeProfit).isEqualTo(BigDecimal("0.00109890"))
        assertThat(profit.orderBookArbitrageProfitHistogram[0]?.usdDepthUpTo).isEqualTo(BigDecimal("100.0"))
        assertThat(profit.orderBookArbitrageProfitHistogram[1]).isNull()
    }

}