package automate.profit.autocoin.exchange.tickerstream

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairs
import automate.profit.autocoin.exchange.ticker.TickerListeners
import automate.profit.autocoin.ticker.TickerDto
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

class TickerSseStreamService(
    private val tickerApiBaseUrl: String,
    private val httpClient: OkHttpClient,
    private val eventSourceFactory: EventSource.Factory,
    private val tickerListeners: TickerListeners,
    private val objectMapper: ObjectMapper,
    private val executorForReconnecting: ExecutorService,
    private val lock: Semaphore = Semaphore(1)
) {
    private companion object : KLogging()

    private val isConnected = AtomicBoolean(false)
    private val isFirstOrderBookLoggedAfterConnect = AtomicBoolean(false)

    fun scheduleReconnectOnFailure(commonExchangeCurrencyPairs: CommonExchangeCurrencyPairs) {
        logger.info { "Scheduling reconnecting ticker stream on failure" }
        executorForReconnecting.submit {
            while (true) {
                startListeningTickerStream(commonExchangeCurrencyPairs)
                Thread.sleep(500)
            }
        }
    }

    private fun onTickerEvent(tickerJson: String) {
        if (!isFirstOrderBookLoggedAfterConnect.compareAndExchange(false, true)) {
            logger.info { "First Ticker event since starting the stream, ticker=$tickerJson" }
        } else {
            logger.debug { "Ticker event=$tickerJson" }
        }
        val tickerDto = objectMapper.readValue(tickerJson, TickerDto::class.java)
        val ticker = tickerDto.toTicker()
        val exchange = SupportedExchange.fromExchangeName(tickerDto.exchange)
        val tickerListener = tickerListeners.getTickerListeners(exchange, ticker.currencyPair)
        tickerListener.forEach {
            try {
                it.onTicker(exchange, ticker.currencyPair, ticker)
            } catch (e: Exception) {
                logger.error(e) { "[$exchange-${ticker.currencyPair}] Error during notifying ticker listener" }
            }
        }
    }

    fun startListeningTickerStream(commonExchangeCurrencyPairs: CommonExchangeCurrencyPairs) {
        lock.acquire()
        logger.info { "Requesting ticker SSE stream" }

        val request = Request.Builder()
                .url("$tickerApiBaseUrl/ticker-sse-stream")
                .build()
        eventSourceFactory.newEventSource(request, object : EventSourceListener() {

            override fun onEvent(eventSource: EventSource, id: String?, type: String?, tickerJson: String) {
                if (type == "start") {
                    logger.info { "Ticker start event received" }
                    if (registerForGettingTickers(commonExchangeCurrencyPairs)) {
                        isConnected.set(true)
                    } else {
                        isConnected.set(false)
                        isFirstOrderBookLoggedAfterConnect.set(false)
                        throw RuntimeException("Could not register for getting tickers")
                    }
                } else {
                    try {
                        onTickerEvent(tickerJson)
                    } catch (e: Exception) {
                        logger.error(e) { "Error during handling ticker event" }
                    }
                }
            }

            override fun onOpen(eventSource: EventSource, response: Response) {
                // on open seems to not work until there is first event sent from producer
                logger.info { "Ticker SSE stream opened" }
            }

            override fun onFailure(eventSource: EventSource, t: Throwable?, response: Response?) {
                val message = "Ticker stream failed, response code=${response?.code}, response=${response?.body?.string()}, client side exception=${t?.message}"
                isConnected.set(false)
                isFirstOrderBookLoggedAfterConnect.set(false)
                eventSource.cancel()
                logger.error(t) { message }
                lock.release()
            }
        })

    }

    private fun registerForGettingTickers(commonExchangeCurrencyPairs: CommonExchangeCurrencyPairs): Boolean {
        logger.info { "Registering for ticker events" }
        val exchangesWithCurrencyPairs = mutableMapOf<String, List<String>>()
        commonExchangeCurrencyPairs.exchangePairsToCurrencyPairs.forEach { (exchangePair, currencyPairs) ->
            val currencyPairStringList = currencyPairs.map { it.toString() }
            exchangesWithCurrencyPairs[exchangePair.firstExchange.exchangeName] = currencyPairStringList
            exchangesWithCurrencyPairs[exchangePair.secondExchange.exchangeName] = currencyPairStringList
        }
        val request = Request.Builder()
                .url("$tickerApiBaseUrl/listener/tickers")
                .header("Content-Type", "application/json")
                .post(objectMapper.writeValueAsString(exchangesWithCurrencyPairs).toRequestBody())
                .build()
        return try {
            val response = httpClient.newCall(request).execute()
            if (response.isSuccessful) {
                logger.info { "Registered for ticker events" }
                true
            } else {
                logger.error { "Could not register for ticker events, code=${response.code}, message=${response.body?.string()}" }
                false
            }
        } catch (e: Exception) {
            logger.error(e) { "Could not register for ticker events" }
            false
        }
    }

    fun isConnected() = isConnected.get()
}
