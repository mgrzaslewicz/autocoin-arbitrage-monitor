package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.ticker.TickerDto
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Duration
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

class TickerFetcher(
        private val tickerApiUrl: String,
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis,
        private val maxTickerAgeMs: Long = Duration.of(1, ChronoUnit.HOURS).toMillis()
) {
    companion object : KLogging()

    private data class TickerWithTimestamp(
            val ticker: Ticker,
            val setAtMillis: Long
    )

    private val tickerCache = ConcurrentHashMap<String, TickerWithTimestamp>()

    fun getTicker(supportedExchange: SupportedExchange, currencyPair: CurrencyPair): Ticker {
        logger.debug { "Requesting $supportedExchange-$currencyPair" }
        logger.debug { "Requesting $supportedExchange-$currencyPair" }
        val request = Request.Builder()
                .url("$tickerApiUrl/ticker/${supportedExchange.exchangeName}/${currencyPair.base}/${currencyPair.counter}")
                .get()
                .build()
        val tickerResponse = httpClient.newCall(request).execute()
        tickerResponse.use {
            check(tickerResponse.code == 200) { "Could not get ticker $supportedExchange-$currencyPair" }

            val tickerDto = objectMapper.readValue(tickerResponse.body?.string(), TickerDto::class.java)
            tickerResponse.body?.close()
            val ticker = tickerDto.toTicker()
            return if (ticker.hasTimestamp()) {
                ticker
            } else {
                ticker.copy(timestamp = Instant.ofEpochMilli(currentTimeMillis()))
            }
        }
    }

    fun getCachedTicker(supportedExchange: SupportedExchange, currencyPair: CurrencyPair): Ticker {
        val cacheKey = "$supportedExchange-$currencyPair"
        if (tickerCache.containsKey(cacheKey)) {
            val valueWithTimestamp = tickerCache[cacheKey]!!
            if (isOlderThanMaxCacheAge(valueWithTimestamp.setAtMillis)) {
                tickerCache.remove(cacheKey)
            }
        }
        return tickerCache.computeIfAbsent(cacheKey) {
            TickerWithTimestamp(
                    setAtMillis = currentTimeMillis(),
                    ticker = getTicker(supportedExchange, currencyPair)
            )
        }.ticker
    }

    private fun isOlderThanMaxCacheAge(setAtMillis: Long): Boolean {
        return currentTimeMillis() - setAtMillis > maxTickerAgeMs
    }

}