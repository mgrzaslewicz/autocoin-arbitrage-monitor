package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.orderbook.OrderBook
import automate.profit.autocoin.exchange.orderbook.OrderBookListener
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import automate.profit.autocoin.metrics.MetricsService
import mu.KLogging
import kotlin.system.measureTimeMillis

/**
 * Calculates arbitrage opportunities based on order books
 */
class TwoLegOrderBookArbitrageMonitor(
        private val currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        private val profitCache: TwoLegOrderBookArbitrageProfitCache,
        private val profitCalculator: TwoLegOrderBookArbitrageProfitCalculator,
        private val metricsService: MetricsService
) {
    private companion object : KLogging()

    private val currencyPair = currencyPairWithExchangePair.currencyPair
    private val exchangePair = currencyPairWithExchangePair.exchangePair
    private val commonTags = "currencyPair=$currencyPair,exchanges=${exchangePair.firstExchange.exchangeName}-${exchangePair.secondExchange.exchangeName}"
    private var firstExchangeOrderBook: OrderBook? = null
    private var secondExchangeOrderBook: OrderBook? = null

    private fun onFirstExchangeOrderBook(orderBook: OrderBook) {
        firstExchangeOrderBook = orderBook
        metricsService.recordArbitrageOrderbooksSize(orderBook.buyOrders.size.toLong(), orderBook.sellOrders.size.toLong(), commonTags)
        onOrderBooks()
    }

    private fun onSecondExchangeOrderBook(orderBook: OrderBook) {
        secondExchangeOrderBook = orderBook
        onOrderBooks()
    }

    private fun onOrderBooks() {
        if (firstExchangeOrderBook != null) {
            if (secondExchangeOrderBook != null) {
                val orderBookPair = OrderBookPair(firstExchangeOrderBook!!, secondExchangeOrderBook!!)
                val millis = measureTimeMillis {
                    val profit = profitCalculator.calculateProfit(currencyPairWithExchangePair, orderBookPair)
                    if (profit == null) {
                        logger.debug { "No profit found for $currencyPairWithExchangePair" }
                        profitCache.removeProfit(currencyPairWithExchangePair)
                    } else {
                        profitCache.setProfit(profit)
                    }
                }
                metricsService.recordArbitrageProfitCalculationTime(millis, commonTags)

            } else {
                logger.debug { "Null secondOrderBook for $currencyPairWithExchangePair" }
            }
        } else {
            logger.debug { "Null firstOrderBook for $currencyPairWithExchangePair" }
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

}