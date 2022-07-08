package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.orderbook.OrderBook
import automate.profit.autocoin.exchange.orderbook.OrderBookListener
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerListener
import automate.profit.autocoin.exchange.ticker.TickerPair
import mu.KLogging

/**
 * Calculates arbitrage opportunities based on order books
 */
class TwoLegArbitrageOpportunitiesMonitor(
    val currencyPairWithExchangePair: CurrencyPairWithExchangePair,
    private val profitCache: TwoLegArbitrageProfitOpportunityCache,
    private val profitCalculator: TwoLegArbitrageProfitOpportunityCalculator,
) {
    private companion object : KLogging()

    private val currencyPair = currencyPairWithExchangePair.currencyPair
    private val exchangePair = currencyPairWithExchangePair.exchangePair
    private var firstExchangeOrderBook: OrderBook? = null
    private var firstExchangeTicker: Ticker? = null
    private var secondExchangeOrderBook: OrderBook? = null
    private var secondExchangeTicker: Ticker? = null

    private var firstTimeFirstExchangeOrderBook = true
    private var firstTimeFirstExchangeTicker = true
    private var firstTimeSecondExchangeOrderBook = true
    private var firstTimeSecondExchangeTicker = true

    private fun onFirstExchangeOrderBook(orderBook: OrderBook) {
        if (firstTimeFirstExchangeOrderBook) {
            logger.info { "[${exchangePair.firstExchange}-${currencyPair}] First time onFirstExchangeOrderBook of $currencyPairWithExchangePair. Contains ${orderBook.buyOrders.size} buy orders and ${orderBook.sellOrders.size} sell orders" }
            firstTimeFirstExchangeOrderBook = false
        } else {
            logger.debug { "[${exchangePair.firstExchange}-${currencyPair}] onFirstExchangeOrderBook of $currencyPairWithExchangePair. Contains ${orderBook.buyOrders.size} buy orders and ${orderBook.sellOrders.size} sell orders" }
        }
        firstExchangeOrderBook = orderBook
        recalculateProfit()
    }

    private fun onSecondExchangeOrderBook(orderBook: OrderBook) {
        if (firstTimeSecondExchangeOrderBook) {
            logger.info { "[${exchangePair.secondExchange}-${currencyPair}] First time onSecondExchangeOrderBook of $currencyPairWithExchangePair. Contains ${orderBook.buyOrders.size} buy orders and ${orderBook.sellOrders.size} sell orders" }
            firstTimeSecondExchangeOrderBook = false
        }
        logger.debug { "[${exchangePair.secondExchange}-${currencyPair}] onSecondExchangeOrderBook of $currencyPairWithExchangePair. Contains ${orderBook.buyOrders.size} buy orders and ${orderBook.sellOrders.size} sell orders" }
        secondExchangeOrderBook = orderBook
        recalculateProfit()
    }

    private fun onFirstExchangeTicker(ticker: Ticker) {
        if (firstTimeFirstExchangeTicker) {
            logger.info { "[${exchangePair.firstExchange}-${currencyPair}] First time onFirstExchangeTicker of $currencyPairWithExchangePair" }
            firstTimeFirstExchangeTicker = false
        } else {
            logger.debug { "[${exchangePair.firstExchange}-${currencyPair}] onFirstExchangeTicker of $currencyPairWithExchangePair" }
        }
        firstExchangeTicker = ticker
        recalculateProfit()
    }

    private fun onSecondExchangeTicker(ticker: Ticker) {
        if (firstTimeSecondExchangeTicker) {
            logger.info { "[${exchangePair.firstExchange}-${currencyPair}] First time onSecondExchangeTicker of $currencyPairWithExchangePair" }
            firstTimeSecondExchangeTicker = false
        }
        logger.debug { "[${exchangePair.firstExchange}-${currencyPair}] onSecondExchangeTicker of $currencyPairWithExchangePair" }
        secondExchangeTicker = ticker
        recalculateProfit()
    }

    private fun recalculateProfit() {
        val areBothOrderBooksPresent = checkIfBothOrderBooksPresent()
        if (areBothOrderBooksPresent) {
            val orderBookPair = OrderBookPair(firstExchangeOrderBook!!, secondExchangeOrderBook!!)
            val tickerPair = TickerPair(firstExchangeTicker, secondExchangeTicker)
            val profit = profitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair, tickerPair)
            if (profit == null) {
                logger.debug { "No profit found for $currencyPairWithExchangePair" }
                profitCache.removeProfitOpportunity(currencyPairWithExchangePair)
            } else {
                profitCache.setProfitOpportunity(profit)
            }
        }
    }

    private fun checkIfBothOrderBooksPresent(): Boolean {
        return when {
            firstExchangeOrderBook == null -> {
                logger.debug { "Null firstOrderBook at ${exchangePair.firstExchange} for pair $currencyPairWithExchangePair" }
                false
            }
            secondExchangeOrderBook == null -> {
                logger.debug { "Null secondOrderBook at ${exchangePair.secondExchange} for pair $currencyPairWithExchangePair" }
                false
            }
            else -> true
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
