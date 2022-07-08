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
    private val commonMetricsTags = "currencyPair=$currencyPair,exchanges=${exchangePair.firstExchange.exchangeName}-${exchangePair.secondExchange.exchangeName}"
    private var firstExchangeOrderBook: OrderBook? = null
    private var firstExchangeTicker: Ticker? = null
    private var secondExchangeOrderBook: OrderBook? = null
    private var secondExchangeTicker: Ticker? = null

    private fun onFirstExchangeOrderBook(orderBook: OrderBook) {
        logger.debug { "[${exchangePair.firstExchange}-${currencyPair}] onFirstExchangeOrderBook of $currencyPairWithExchangePair" }
        firstExchangeOrderBook = orderBook
        metricsService.recordArbitrageOrderbooksSize(orderBook.buyOrders.size.toLong(), orderBook.sellOrders.size.toLong(), commonMetricsTags)
        recalculateProfit()
    }

    private fun onSecondExchangeOrderBook(orderBook: OrderBook) {
        logger.debug { "[${exchangePair.secondExchange}-${currencyPair}] onSecondExchangeOrderBook of $currencyPairWithExchangePair" }
        secondExchangeOrderBook = orderBook
        recalculateProfit()
    }

    private fun onFirstExchangeTicker(ticker: Ticker) {
        logger.debug { "[${exchangePair.firstExchange}-${currencyPair}] onFirstExchangeOrderBook of $currencyPairWithExchangePair" }
        firstExchangeTicker = ticker
        recalculateProfit()
    }

    private fun onSecondExchangeTicker(ticker: Ticker) {
        logger.debug { "[${exchangePair.firstExchange}-${currencyPair}] onFirstExchangeOrderBook of $currencyPairWithExchangePair" }
        secondExchangeTicker = ticker
        recalculateProfit()
    }

    private fun recalculateProfit() {
        var isAllRequiredDataPresent = true
        if (firstExchangeOrderBook == null) {
            isAllRequiredDataPresent = false
            logger.debug { "Null firstOrderBook at ${currencyPairWithExchangePair.exchangePair.firstExchange} for pair $currencyPairWithExchangePair" }
        }
        if (firstExchangeTicker == null) {
            isAllRequiredDataPresent = false
            logger.debug { "Null firstExchangeTicker at ${currencyPairWithExchangePair.exchangePair.firstExchange} for pair $currencyPairWithExchangePair" }
        }
        if (secondExchangeOrderBook == null) {
            isAllRequiredDataPresent = false
            logger.debug { "Null secondOrderBook at ${currencyPairWithExchangePair.exchangePair.secondExchange} for pair $currencyPairWithExchangePair" }
        }
        if (secondExchangeTicker == null) {
            isAllRequiredDataPresent = false
            logger.debug { "Null secondExchangeTicker at ${currencyPairWithExchangePair.exchangePair.secondExchange} for pair $currencyPairWithExchangePair" }
        }
        if (isAllRequiredDataPresent) {
            val orderBookPair = OrderBookPair(firstExchangeOrderBook!!, secondExchangeOrderBook!!)
            val tickerPair = TickerPair(firstExchangeTicker!!, secondExchangeTicker!!)
                val profit = profitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair, tickerPair)
                if (profit == null) {
                    logger.debug { "No profit found for $currencyPairWithExchangePair" }
                    profitCache.removeProfit(profitCalculator.profitGroup, currencyPairWithExchangePair)
                } else {
                    profitCache.setProfit(profitCalculator.profitGroup, profit)
                }
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