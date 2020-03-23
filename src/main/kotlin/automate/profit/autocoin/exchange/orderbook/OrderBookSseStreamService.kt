package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairs
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.function.Consumer
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget
import javax.ws.rs.sse.InboundSseEvent
import javax.ws.rs.sse.SseEventSource


class OrderBookSseStreamService(
        private val orderBookApiBaseUrl: String,
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper
) {
    private companion object : KLogging()

    private val sseEventSourceBuilder =
            SseEventSource.target(ClientBuilder
                    .newBuilder()
                    .readTimeout(0L, TimeUnit.SECONDS)
                    .build()
                    .register(JacksonJsonProvider::class.java)
                    .property("Authorization", "Bearer XXX")
                    .target("$orderBookApiBaseUrl/order-book-sse-stream")
            )

    fun startListeningOrderBookStream(commonExchangeCurrencyPairs: CommonExchangeCurrencyPairs) {
        logger.info { "Requesting order book SSE stream" }
        sseEventSourceBuilder
                .reconnectingEvery(5, TimeUnit.SECONDS)
                .build()
                .use { source ->
                    source.register(
                            { sseEvent -> logger.info { "New event: name=${sseEvent.name}, data=${sseEvent.readData()}" } },
                            { error -> logger.error(error) { "Error while streaming" } },
                            { logger.info { "Event stream completed" } }
                    )
                    source.open()
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