package automate.profit.autocoin.exchange.orderbookstream

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairs
import automate.profit.autocoin.exchange.orderbook.OrderBookListenersProvider
import automate.profit.autocoin.order.OrderBookResponseDto
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.sse.EventSource
import okhttp3.sse.EventSourceListener
import java.util.concurrent.ExecutorService
import java.util.concurrent.Semaphore
import java.util.concurrent.atomic.AtomicBoolean

class OrderBookSseStreamService(
        private val orderBookApiBaseUrl: String,
        private val httpClient: OkHttpClient,
        private val eventSourceFactory: EventSource.Factory,
        private val orderBookListenersProvider: OrderBookListenersProvider,
        private val objectMapper: ObjectMapper,
        private val executorForReconnecting: ExecutorService,
        private val lock: Semaphore = Semaphore(1)
) {
    private companion object : KLogging()

    private val isConnected = AtomicBoolean(false)
    private val isFirstOrderBookLoggedAfterConnect = AtomicBoolean(false)

    fun scheduleReconnectOnFailure(commonExchangeCurrencyPairs: CommonExchangeCurrencyPairs) {
        logger.info { "Scheduling reconnecting order book stream on failure" }
        executorForReconnecting.submit {
            while (true) {
                startListeningOrderBookStream(commonExchangeCurrencyPairs)
                Thread.sleep(500)
            }
        }
    }

    private fun onOrderBookEvent(orderBookJson: String) {
        if (!isFirstOrderBookLoggedAfterConnect.compareAndExchange(false, true)) {
            logger.info { "First orderBook event since starting the stream" }
        } else {
            logger.debug { "OrderBook event=$orderBookJson" }
        }
        val orderBookDto = objectMapper.readValue(orderBookJson, OrderBookResponseDto::class.java)
        val orderBook = orderBookDto.toOrderBook()
        val exchange = SupportedExchange.fromExchangeName(orderBookDto.exchangeName)
        val currencyPair = CurrencyPair.of(orderBookDto.currencyPair)
        val orderBookListener = orderBookListenersProvider.getOrderBookListeners(exchange, currencyPair)
        orderBookListener.forEach {
            try {
                it.onOrderBook(exchange, currencyPair, orderBook)
            } catch (e: Exception) {
                logger.error(e) { "[$exchange-$currencyPair] Error during onOrderBook" }
            }
        }
    }

    fun startListeningOrderBookStream(commonExchangeCurrencyPairs: CommonExchangeCurrencyPairs) {
        lock.acquire()
        logger.info { "Requesting order book SSE stream" }

        val request = Request.Builder()
                .url("$orderBookApiBaseUrl/order-book-sse-stream")
                .build()
        eventSourceFactory.newEventSource(request, object : EventSourceListener() {

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, orderBookJson: String) {
                if (type == "start") {
                    logger.info { "Order book start event received" }
                    if (registerForGettingOrderBooks(commonExchangeCurrencyPairs)) {
                        isConnected.set(true)
                    } else {
                        isConnected.set(false)
                        isFirstOrderBookLoggedAfterConnect.set(false)
                        throw RuntimeException("Could not register for getting order books")
                    }
                } else {
                    try {
                        onOrderBookEvent(orderBookJson)
                    } catch (e: Exception) {
                        logger.error(e) { "Error during handling order book event" }
                    }
                }
            }

            override fun onOpen(eventSource: EventSource, response: Response) {
                // on open seems to not work until there is first event sent from producer
                logger.info { "Order book SSE stream opened" }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val message = "Order book stream failed, response code=${response?.code}, response=${response?.body?.string()}, client side exception=${t?.message}"
                isConnected.set(false)
                eventSource.cancel()
                logger.error(t) { message }
                lock.release()
            }
        })

    }

    private fun registerForGettingOrderBooks(commonExchangeCurrencyPairs: CommonExchangeCurrencyPairs): Boolean {
        logger.info { "Registering for order book events" }
        val exchangesWithCurrencyPairs = mutableMapOf<String, List<String>>()
        commonExchangeCurrencyPairs.exchangePairsToCurrencyPairs.forEach { (exchangePair, currencyPairs) ->
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
                logger.info { "Registered for order book events" }
                true
            } else {
                logger.error { "Could not register for order books, code=${response.code}, message=${response.body?.string()}" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Could not register for order books" }
            false
        }
    }

    fun isConnected() = isConnected.get()
}