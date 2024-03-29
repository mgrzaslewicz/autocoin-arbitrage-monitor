package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.app.config.ExchangePair
import automate.profit.autocoin.exchange.CurrencyPrice
import automate.profit.autocoin.exchange.RestPriceService
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.order.ExchangeOrderType
import automate.profit.autocoin.exchange.orderbook.OrderBook
import automate.profit.autocoin.exchange.orderbook.OrderBookExchangeOrder
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerPair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import java.math.BigDecimal

class TwoLegOrderBookArbitrageProfitCalculatorTest {
    private val currencyPair = CurrencyPair.of("X/Y")
    private val exchangeA = BITTREX
    private val exchangeB = BINANCE
    private val exchangePair = ExchangePair(exchangeA, exchangeB)
    private val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
    private val usdValueFromPriceService = BigDecimal("2000.0")
    private val pricesService = mock<RestPriceService>().apply {
        whenever(getUsdValue(eq("Y"), any())).thenReturn(usdValueFromPriceService)
        whenever(getUsdPrice(eq("Y"))).thenReturn(
            CurrencyPrice(
                price = BigDecimal(0.3),
                baseCurrency = "A",
                counterCurrency = "B",
                timestampMillis = System.currentTimeMillis(),
            )
        )
    }
    private val bigDecimalWhichDoesNotMatter = BigDecimal.ONE
    private val counterCurrency24hVolume = BigDecimal("10.0")
    private val receivedAtMillis = System.currentTimeMillis()
    private val exchangeATicker = Ticker(
        currencyPair = currencyPair,
        ask = bigDecimalWhichDoesNotMatter,
        bid = bigDecimalWhichDoesNotMatter,
        baseCurrency24hVolume = bigDecimalWhichDoesNotMatter,
        counterCurrency24hVolume = counterCurrency24hVolume,
        receivedAtMillis = receivedAtMillis, exchangeTimestampMillis = null,
    )
    private val exchangeBTicker = Ticker(
        currencyPair = currencyPair,
        ask = bigDecimalWhichDoesNotMatter,
        bid = bigDecimalWhichDoesNotMatter,
        baseCurrency24hVolume = bigDecimalWhichDoesNotMatter,
        counterCurrency24hVolume = BigDecimal("10.0"),
        receivedAtMillis = receivedAtMillis, exchangeTimestampMillis = null,
    )
    private val tickerPair = TickerPair(first = exchangeATicker, second = exchangeBTicker)

    private val orderBookUsdAmountThresholds = listOf(BigDecimal("100.0"), BigDecimal("500.0"))
    private val twoLegArbitrageProfitCalculator = TwoLegArbitrageProfitOpportunityCalculator(
        opportunityCutOff = TwoLegArbitrageProfitOpportunityCutOff(),
        priceService = pricesService,
        orderBookUsdAmountThresholds = orderBookUsdAmountThresholds,
        relativeProfitCalculator = TestTwoLegArbitrageProfitCalculator(),
        metricsService = mock(),
    )
    private val buyOrderExchangeA = OrderBookExchangeOrder(
        exchangeName = "exchangeA",
        type = ExchangeOrderType.BID_BUY,
        orderedAmount = 500.toBigDecimal(),
        price = 7200.toBigDecimal(),
        currencyPair = currencyPair,
        receivedAtMillis = receivedAtMillis, exchangeTimestampMillis = null,
    )
    private val buyOrderExchangeB = buyOrderExchangeA.copy(
        exchangeName = "exchangeB"
    )
    private val sellOrderExchangeB = buyOrderExchangeB.copy(type = ExchangeOrderType.ASK_SELL)
    private val orderListDoesNotMatter: List<OrderBookExchangeOrder> = listOf(buyOrderExchangeA)

    @Test
    fun shouldFindNoProfitWhenOrderTooOld() {
        // given
        val twoLegArbitrageProfitCalculator = TwoLegArbitrageProfitOpportunityCalculator(
            opportunityCutOff = TwoLegArbitrageProfitOpportunityCutOff(),
            priceService = mock(),
            orderBookUsdAmountThresholds = mock(),
            staleOrderBooksDetector = mock<StaleOrderBooksDetector>().apply { whenever(this.orderBooksAreTooOld(any())).thenReturn(true) },
            staleTickerDetector = mock<StaleTickerDetector>().apply { whenever(this.oneOfTickersIsTooOld(any())).thenReturn(false) },
            relativeProfitCalculator = mock(),
            metricsService = mock(),
        )
        // then
        assertThat(
            twoLegArbitrageProfitCalculator.calculateProfit(
                currencyPairWithExchangePair = mock(), orderBookPair = mock(), tickerPair = mock()
            )
        ).isNull()
    }

    @Test
    fun shouldFindNoProfitWhenTickerTooOld() {
        // given
        val twoLegArbitrageProfitCalculator = TwoLegArbitrageProfitOpportunityCalculator(
            opportunityCutOff = TwoLegArbitrageProfitOpportunityCutOff(),
            priceService = mock(),
            orderBookUsdAmountThresholds = mock(),
            staleOrderBooksDetector = mock<StaleOrderBooksDetector>().apply { whenever(this.orderBooksAreTooOld(any())).thenReturn(false) },
            staleTickerDetector = mock<StaleTickerDetector>().apply { whenever(this.oneOfTickersIsTooOld(any())).thenReturn(true) },
            relativeProfitCalculator = mock(),
            metricsService = mock(),
        )
        // then
        assertThat(
            twoLegArbitrageProfitCalculator.calculateProfit(
                currencyPairWithExchangePair = mock(), orderBookPair = mock(), tickerPair = mock()
            )
        ).isNull()
    }


    @Test
    fun shouldFindNoProfitWhenSpreadTooSmall() {
        // given
        val orderBookPair = OrderBookPair(
            first = OrderBook(
                buyOrders = listOf(buyOrderExchangeA), sellOrders = listOf(buyOrderExchangeA),
                receivedAtMillis = receivedAtMillis, exchangeTimestampMillis = null,
            ), second = OrderBook(
                buyOrders = listOf(buyOrderExchangeB), sellOrders = listOf(buyOrderExchangeB),
                receivedAtMillis = receivedAtMillis, exchangeTimestampMillis = null,
            )
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair, tickerPair)
        // then
        assertThat(profit).isNull()
    }

    @Test
    fun shouldSellAtExchangeB() {
        // given
        // sell higher at exchange A, buy lower at exchange B which means
        // buyPrice(A) > sellPrice(B)
        val orderBookPair = OrderBookPair(
            first = OrderBook(
                receivedAtMillis = receivedAtMillis, exchangeTimestampMillis = null,
                buyOrders = listOf(
                    buyOrderExchangeA.copy(
                        price = BigDecimal("7200"), orderedAmount = BigDecimal("400"),
                        receivedAtMillis = receivedAtMillis, exchangeTimestampMillis = null,
                    )
                ), sellOrders = orderListDoesNotMatter
            ), second = OrderBook(
                receivedAtMillis = receivedAtMillis, exchangeTimestampMillis = null,
                buyOrders = orderListDoesNotMatter, sellOrders = listOf(
                    sellOrderExchangeB.copy(
                        price = BigDecimal("7150"), orderedAmount = BigDecimal("400"),
                        receivedAtMillis = receivedAtMillis, exchangeTimestampMillis = null,
                    )
                )
            )
        )
        // when
        val profit = twoLegArbitrageProfitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair, tickerPair)
        // then
        assertThat(profit).isNotNull
        with(profit!!) {
            assertThat(currencyPairWithExchangePair).isEqualTo(currencyPairWithExchangePair)
            assertThat(usd24hVolumeAtBuyExchange).isEqualTo(usdValueFromPriceService)
            assertThat(usd24hVolumeAtSellExchange).isEqualTo(usdValueFromPriceService)
            assertThat(buyAtExchange).isEqualTo(exchangeB)
            assertThat(sellAtExchange).isEqualTo(exchangeA)
            assertThat(profitOpportunityHistogram).hasSize(orderBookUsdAmountThresholds.size)
            assertThat(profitOpportunityHistogram[0]?.sellPrice).isEqualTo(BigDecimal("7200.00000000"))
            assertThat(profitOpportunityHistogram[0]?.buyPrice).isEqualTo(BigDecimal("7150.00000000"))
            assertThat(profitOpportunityHistogram[0]?.relativeProfit).isEqualTo(BigDecimal("0.00699301"))
            assertThat(profitOpportunityHistogram[0]?.usdDepthUpTo).isEqualTo(BigDecimal("100.0"))
            assertThat(profitOpportunityHistogram[1]).isNotNull
            assertThat(profitOpportunityHistogram[1]!!.usdDepthUpTo).isEqualTo(BigDecimal("500.0"))
        }
    }

}
