package automate.profit.autocoin.api.health

import automate.profit.autocoin.exchange.ExchangeWithCurrencyPair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.ExchangePairWithOpportunityCount
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCache
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.exchange.orderbook.OrderBook
import automate.profit.autocoin.exchange.orderbook.OrderBookListener
import automate.profit.autocoin.exchange.orderbook.OrderBookListeners
import automate.profit.autocoin.exchange.orderbookstream.OrderBookSseStreamService
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerListener
import automate.profit.autocoin.exchange.ticker.TickerListeners
import automate.profit.autocoin.exchange.tickerstream.TickerSseStreamService
import java.util.*

data class TwoLegArbitrageOpportunitiesCount(
    val currentTotalCount: Long,
    val exchangePairsWithOpportunityCount: List<ExchangePairWithOpportunityCount>,
)

data class Health(
    val healthy: Boolean,
    val unhealthyReasons: List<String>,
    val twoLegArbitrageOpportunities: TwoLegArbitrageOpportunitiesCount,
    val receivedOrderBooksSinceStart: Map<SupportedExchange, Long>,
    val receivedTickersSinceStart: Map<SupportedExchange, Long>,
    val commonCurrencyPairs: Map<String, Int>,
)

class HealthService(
    private val orderBookSseStreamService: OrderBookSseStreamService,
    private val tickerSseStreamService: TickerSseStreamService,
    private val commonExchangeCurrencyPairsService: CommonExchangeCurrencyPairsService,
    private val twoLegArbitrageProfitOpportunityCache: TwoLegArbitrageProfitOpportunityCache,
) {
    private val orderBookUpdatesSinceStart = EnumMap<SupportedExchange, Long>(SupportedExchange::class.java).apply {
        SupportedExchange.values().forEach { put(it, 0L) }
    }
    private val tickerUpdatesSinceStart = EnumMap<SupportedExchange, Long>(SupportedExchange::class.java).apply {
        SupportedExchange.values().forEach { put(it, 0L) }
    }

    fun addOrderBookListenersTo(orderBookListeners: OrderBookListeners) {
        val orderBookListener = object : OrderBookListener {
            override fun onOrderBook(exchange: SupportedExchange, currencyPair: CurrencyPair, orderBook: OrderBook) {
                this@HealthService.onOrderBook(exchange, currencyPair, orderBook)
            }
        }
        commonExchangeCurrencyPairsService.lastCalculatedCommonExchangeCurrencyPairs.currencyPairsToExchangePairs.forEach {
            it.value.map { exchangePair ->
                orderBookListeners.addOrderBookListener(ExchangeWithCurrencyPair(exchange = exchangePair.firstExchange, currencyPair = it.key), orderBookListener)
                orderBookListeners.addOrderBookListener(ExchangeWithCurrencyPair(exchange = exchangePair.secondExchange, currencyPair = it.key), orderBookListener)
            }
        }
    }

    fun addTickerListenersTo(tickerListeners: TickerListeners) {
        val orderBookListener = object : TickerListener {
            override fun onTicker(exchange: SupportedExchange, currencyPair: CurrencyPair, ticker: Ticker) {
                this@HealthService.onTicker(exchange, currencyPair, ticker)
            }
        }
        commonExchangeCurrencyPairsService.lastCalculatedCommonExchangeCurrencyPairs.currencyPairsToExchangePairs.forEach {
            it.value.map { exchangePair ->
                tickerListeners.addTickerListener(ExchangeWithCurrencyPair(exchange = exchangePair.firstExchange, currencyPair = it.key), orderBookListener)
                tickerListeners.addTickerListener(ExchangeWithCurrencyPair(exchange = exchangePair.secondExchange, currencyPair = it.key), orderBookListener)
            }
        }
    }

    private fun onOrderBook(exchange: SupportedExchange, currencyPair: CurrencyPair, orderBook: OrderBook) {
        orderBookUpdatesSinceStart[exchange] = orderBookUpdatesSinceStart[exchange]!! + 1
    }


    private fun onTicker(exchange: SupportedExchange, currencyPair: CurrencyPair, ticker: Ticker) {
        tickerUpdatesSinceStart[exchange] = orderBookUpdatesSinceStart[exchange]!! + 1
    }

    fun getHealth(): Health {
        val twoLegArbitrageExchangePairsOpportunityCount = twoLegArbitrageProfitOpportunityCache.getExchangePairsOpportunityCount()
        val isConnectedToTickerStream = tickerSseStreamService.isConnected()
        val isConnectedToOrderBookStream = orderBookSseStreamService.isConnected()
        val health = Health(
            healthy = isConnectedToTickerStream && isConnectedToOrderBookStream,
            unhealthyReasons = listOfNotNull(
                if (isConnectedToTickerStream) null else "Not connected to ticker stream",
                if (isConnectedToOrderBookStream) null else "Not connected to order book stream",
            ),
            commonCurrencyPairs = commonExchangeCurrencyPairsService.lastCalculatedCommonExchangeCurrencyPairs.exchangePairsToCurrencyPairs.map {
                "${it.key.firstExchange}/${it.key.secondExchange}" to it.value.size
            }
                .sortedBy { it.second }
                .toMap(),
            receivedOrderBooksSinceStart = Collections.unmodifiableMap(orderBookUpdatesSinceStart.toList().sortedBy { it.second }.toMap()),
            receivedTickersSinceStart = Collections.unmodifiableMap(tickerUpdatesSinceStart.toList().sortedBy { it.second }.toMap()),
            twoLegArbitrageOpportunities = TwoLegArbitrageOpportunitiesCount(
                exchangePairsWithOpportunityCount = twoLegArbitrageExchangePairsOpportunityCount,
                currentTotalCount = twoLegArbitrageExchangePairsOpportunityCount.sumOf { it.opportunityCount }
            )
        )
        return health
    }
}
