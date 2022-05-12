package automate.profit.autocoin.api

import automate.profit.autocoin.exchange.ExchangeWithCurrencyPair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.exchange.orderbook.OrderBook
import automate.profit.autocoin.exchange.orderbook.OrderBookListener
import automate.profit.autocoin.exchange.orderbook.OrderBookListeners
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerListener
import automate.profit.autocoin.exchange.ticker.TickerListeners
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.util.Methods.GET
import java.util.*

data class HealthDto(
    val commonCurrencyPairs: Map<String, Int>,
    val orderBookUpdatesSinceStart: Map<SupportedExchange, Long>,
    val tickerUpdatesSinceStart: Map<SupportedExchange, Long>,
)

class HealthService(
    private val commonExchangeCurrencyPairsService: CommonExchangeCurrencyPairsService,
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

    fun getHealth(): HealthDto {
        val health = HealthDto(
            commonCurrencyPairs = commonExchangeCurrencyPairsService.lastCalculatedCommonExchangeCurrencyPairs.exchangePairsToCurrencyPairs.map {
                "${it.key.firstExchange}/${it.key.secondExchange}" to it.value.size
            }
                .sortedBy { it.second }
                .toMap(),
            orderBookUpdatesSinceStart = Collections.unmodifiableMap(orderBookUpdatesSinceStart.toList().sortedBy { it.second }.toMap()),
            tickerUpdatesSinceStart = Collections.unmodifiableMap(tickerUpdatesSinceStart.toList().sortedBy { it.second }.toMap()),
        )
        return health
    }
}

class HealthController(
    private val healthService: HealthService,
    private val objectMapper: ObjectMapper,
) : ApiController {
    private fun getHealth() = object : ApiHandler {
        override val method = GET
        override val urlTemplate = "/health"

        override val httpHandler = HttpHandler { httpServerExchange ->
            httpServerExchange.responseSender.send(objectMapper.writeValueAsString(healthService.getHealth()))
        }
    }

    override fun apiHandlers(): List<ApiHandler> = listOf(getHealth())
}
