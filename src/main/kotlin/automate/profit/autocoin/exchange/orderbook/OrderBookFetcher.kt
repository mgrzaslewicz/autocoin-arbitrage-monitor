package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.order.UserExchangeOrderBookService
import automate.profit.autocoin.order.OrderBookResponseDto
import com.fasterxml.jackson.databind.ObjectMapper
import com.timgroup.statsd.StatsDClient
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request

class OrderBookFetcher(
        private val supportedExchange: SupportedExchange,
        private val orderBookApiUrl: String,
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        private val statsDClient: StatsDClient
) : UserExchangeOrderBookService {
    private companion object : KLogging()

    override fun getOrderBook(currencyPair: CurrencyPair): OrderBook {
        logger.debug { "Requesting $supportedExchange-$currencyPair" }
        // assumption is order book is sorted by price (descending for buy orders, ascending for sell orders)
        val millisBefore = System.currentTimeMillis()

        val request = Request.Builder()
                .url("$orderBookApiUrl/order-book/${supportedExchange.exchangeName}/${currencyPair.base}/${currencyPair.counter}")
                .get()
                .build()
        val orderBookResponse = httpClient.newCall(request).execute()
        orderBookResponse.use {
            check(orderBookResponse.code == 200) { "Could not get order book $supportedExchange-$currencyPair" }

            val orderBookDto = objectMapper.readValue(orderBookResponse.body?.string(), OrderBookResponseDto::class.java)
            orderBookResponse.body?.close()

            val millisAfter = System.currentTimeMillis()
            statsDClient.recordExecutionTime("fetchOrderBook", millisAfter - millisBefore, currencyPair.toString(), supportedExchange.exchangeName)
            return orderBookDto.toOrderBook()
        }
    }
}