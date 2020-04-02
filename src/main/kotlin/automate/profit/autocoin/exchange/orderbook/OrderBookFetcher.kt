package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.order.ExchangeOrderBookService
import automate.profit.autocoin.order.OrderBookResponseDto
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request

class OrderBookFetcher(
        private val orderBookApiUrl: String,
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper
) : ExchangeOrderBookService {
    private companion object : KLogging()

    override fun getOrderBook(exchangeName: String, currencyPair: CurrencyPair): OrderBook {
        logger.debug { "[$exchangeName-$currencyPair] Requesting order book" }
        val request = Request.Builder()
                .url("$orderBookApiUrl/order-book/${exchangeName}/${currencyPair.base}/${currencyPair.counter}")
                .get()
                .build()
        val orderBookResponse = httpClient.newCall(request).execute()
        orderBookResponse.use {
            check(orderBookResponse.isSuccessful) { "[$exchangeName-$currencyPair] Could not get order book" }

            val orderBookDto = objectMapper.readValue(orderBookResponse.body?.string(), OrderBookResponseDto::class.java)
            orderBookResponse.body?.close()

            return orderBookDto.toOrderBook()
        }
    }
}