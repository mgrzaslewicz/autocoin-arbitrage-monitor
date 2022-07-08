package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairs
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class OrderBookSseStreamService(
        private val orderBookApiBaseUrl: String,
        private val httpClient: OkHttpClient,
        private val eventSourceFactory: EventSource.Factory,
        private val objectMapper: ObjectMapper
) {
    private companion object : KLogging()

    fun startListeningOrderBookStream(commonExchangeCurrencyPairs: CommonExchangeCurrencyPairs) {
        logger.info { "Requesting order book SSE stream" }
        val countDownLatch = CountDownLatch(1)

        val request = Request.Builder()
                .url("$orderBookApiBaseUrl/order-book-sse-stream")
                .build()
        eventSourceFactory.newEventSource(request, object : EventSourceListener() {

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, data: String) {
                logger.info { "New event: type=$type, data=$data" }
            }

            override fun onOpen(eventSource: EventSource, response: Response) {
                logger.info { "Order book SSE stream opened" }
                if (registerForGettingOrderBooks(commonExchangeCurrencyPairs)) {
                    countDownLatch.countDown()
                }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                // TODO figure out why there is java.net.SocketTimeoutException: timeout after opening stream
                val message = "Could not request order book stream, response code=${response?.code}, response=${response?.body?.string()}, client side exception=${t?.message}"
                eventSource.cancel()
                logger.error(t) { message }
            }
        })
        if (!countDownLatch.await(5, TimeUnit.SECONDS)) {
            logger.error { "Registering for order book stream failed" }
//            throw RuntimeException("Registering for order book stream failed")
        }
    }

    private fun registerForGettingOrderBooks(commonExchangeCurrencyPairs: CommonExchangeCurrencyPairs): Boolean {
        logger.info { "Registering for order books" }
        val exchangesWithCurrencyPairs = mutableMapOf<String, List<String>>()
        commonExchangeCurrencyPairs.exchangePairsToCurrencyPairs.forEach { exchangePair, currencyPairs ->
            val currencyPairStringList = currencyPairs.map { it.toString() }
            exchangesWithCurrencyPairs[exchangePair.firstExchange.exchangeName] = currencyPairStringList
            exchangesWithCurrencyPairs[exchangePair.secondExchange.exchangeName] = currencyPairStringList
        }
        val request = Request.Builder()
                .url("$orderBookApiBaseUrl/listener/order-books")
                .header("Content-Type", "application/json")
                .post(objectMapper.writeValueAsString(exchangesWithCurrencyPairs).toRequestBody())
                .build()
        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                true
            } else {
                logger.error { "Could not register for order books, code=${response.code}" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Could not register for order books" }
            false
        }
    }
}