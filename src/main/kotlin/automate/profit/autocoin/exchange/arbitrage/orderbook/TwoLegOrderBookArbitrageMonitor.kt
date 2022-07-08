package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.orderbook.OrderBook
import automate.profit.autocoin.exchange.orderbook.OrderBookListener
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerListener
import automate.profit.autocoin.exchange.ticker.TickerPair
import automate.profit.autocoin.metrics.MetricsService
import mu.KLogging
import kotlin.system.measureTimeMillis

/**
 * Calculates arbitrage opportunities based on order books
 */
class TwoLegOrderBookArbitrageMonitor(
        val currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        private val profitCache: TwoLegOrderBookArbitrageProfitCache,
        private val profitCalculator: TwoLegOrderBookArbitrageProfitCalculator,
        private val metricsService: MetricsService
) {
    private companion object : KLogging()

    private val currencyPair = currencyPairWithExchangePair.currencyPair
    private val exchangePair = currencyPairWithExchangePair.exchangePair
    private val commonTags = "currencyPair=$currencyPair,exchanges=${exchangePair.firstExchange.exchangeName}-${exchangePair.secondExchange.exchangeName}"
    private var firstExchangeOrderBook: OrderBook? = null
    private var firstExchangTicker: Ticker? = null
    private var secondExchangeOrderBook: OrderBook? = null
    private var secondExchangeTicker: Ticker? = null

    private fun onFirstExchangeOrderBook(orderBook: OrderBook) {
        logger.debug { "[${exchangePair.firstExchange}-${currencyPair}] onFirstExchangeOrderBook of $currencyPairWithExchangePair" }
        firstExchangeOrderBook = orderBook
        metricsService.recordArbitrageOrderbooksSize(orderBook.buyOrders.size.toLong(), orderBook.sellOrders.size.toLong(), commonTags)
        recalculateProfit()
    }

    private fun onSecondExchangeOrderBook(orderBook: OrderBook) {
        logger.debug { "[${exchangePair.secondExchange}-${currencyPair}] onSecondExchangeOrderBook of $currencyPairWithExchangePair" }
        secondExchangeOrderBook = orderBook
        recalculateProfit()
    }

    private fun onFirstExchangeTicker(ticker: Ticker) {
        logger.debug { "[${exchangePair.firstExchange}-${currencyPair}] onFirstExchangeOrderBook of $currencyPairWithExchangePair" }
        firstExchangTicker = ticker
        recalculateProfit()
    }

    private fun onSecondExchangeTicker(ticker: Ticker) {
        logger.debug { "[${exchangePair.firstExchange}-${currencyPair}] onFirstExchangeOrderBook of $currencyPairWithExchangePair" }
        secondExchangeTicker = ticker
        recalculateProfit()
    }

    private fun recalculateProfit() {
        if (firstExchangeOrderBook != null && firstExchangTicker != null) {
            if (secondExchangeOrderBook != null && secondExchangeTicker != null) {
                val orderBookPair = OrderBookPair(firstExchangeOrderBook!!, secondExchangeOrderBook!!)
                val tickerPair = TickerPair(firstExchangTicker!!, secondExchangeTicker!!)
                val millis = measureTimeMillis {
                    val profit = profitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair, tickerPair)
                    if (profit == null) {
                        logger.debug { "No profit found for $currencyPairWithExchangePair" }
                        profitCache.removeProfit(currencyPairWithExchangePair)
                    } else {
                        profitCache.setProfit(profit)
                    }
                }
                metricsService.recordArbitrageProfitCalculationTime(millis, commonTags)

            } else {
                logger.debug { "Null secondOrderBook  or secondExchangeTicker for $currencyPairWithExchangePair" }
            }
        } else {
            logger.debug { "Null firstOrderBook or firstExchangeTicker for $currencyPairWithExchangePair" }
        }
    }

    fun getOrderBookListeners(): Pair<OrderBookListener, OrderBookListener> = Pair(
            object : OrderBookListener {
                override fun onNoNewOrderBook(exchange: SupportedExchange, currencyPair: CurrencyPair, orderBook: OrderBook?) {
                    if (orderBook != null) onFirstExchangeOrderBook(orderBook)
                }

                override fun onOrderBook(exchange: SupportedExchange, currencyPair: CurrencyPair, orderBook: OrderBook) {
                    onFirstExchangeOrderBook(orderBook)
                }
            },
            object : OrderBookListener {
                override fun onNoNewOrderBook(exchange: SupportedExchange, currencyPair: CurrencyPair, orderBook: OrderBook?) {
                    if (orderBook != null) onSecondExchangeOrderBook(orderBook)
                }

                override fun onOrderBook(exchange: SupportedExchange, currencyPair: CurrencyPair, orderBook: OrderBook) {
                    onSecondExchangeOrderBook(orderBook)
                }
            }
    )

    fun getTickerListeners(): Pair<TickerListener, TickerListener> = Pair(
            object : TickerListener {
                override fun onNoNewTicker(exchange: SupportedExchange, currencyPair: CurrencyPair, ticker: Ticker?) {
                    if (ticker != null) onFirstExchangeTicker(ticker)
                }

                override fun onTicker(exchange: SupportedExchange, currencyPair: CurrencyPair, ticker: Ticker) {
                    onFirstExchangeTicker(ticker)
                }
            },
            object : TickerListener {
                override fun onNoNewTicker(exchange: SupportedExchange, currencyPair: CurrencyPair, ticker: Ticker?) {
                    if (ticker != null) onSecondExchangeTicker(ticker)
                }

                override fun onTicker(exchange: SupportedExchange, currencyPair: CurrencyPair, ticker: Ticker) {
                    onSecondExchangeTicker(ticker)
                }
            }
    )

}