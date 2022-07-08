package automate.profit.autocoin.exchange

import automate.profit.autocoin.logger.PeriodicalLogger
import automate.profit.autocoin.metrics.MetricsService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap


data class CurrencyPriceDto(
    val price: Double,
    val baseCurrency: String,
    val counterCurrency: String
)

class PriceService(
    private val priceApiUrl: String,
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    private val maxPriceCacheAgeMs: Long = Duration.of(1, ChronoUnit.HOURS).toMillis(),
    private val metricsService: MetricsService,
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis
) {

    private data class ValueWithTimestamp(
        val value: BigDecimal,
        val calculatedAtMillis: Long
    )

    private val priceCache = ConcurrentHashMap<String, ValueWithTimestamp>()
    private val currencyLocks = ConcurrentHashMap<String, String>()

    companion object {
        private val logger = PeriodicalLogger(wrapped = KotlinLogging.logger {}).scheduleLogFlush()
    }

    fun getUsdPrice(currencyCode: String): BigDecimal {
        if (currencyCode == "USD") {
            return BigDecimal.ONE
        }
        fetchPrice(currencyCode)
        return priceCache.getValue(currencyCode).value
    }

    private fun fetchPrice(currencyCode: String) {
        synchronized(currencyLocks.computeIfAbsent(currencyCode) { currencyCode }) {
            if (priceCache.containsKey(currencyCode)) {
                val valueWithTimestamp = priceCache[currencyCode]!!
                if (isOlderThanMaxCacheAge(valueWithTimestamp.calculatedAtMillis)) {
                    priceCache.remove(currencyCode)
                }
            }

            priceCache.computeIfAbsent(currencyCode) {
                ValueWithTimestamp(
                    calculatedAtMillis = currentTimeMillisFunction(),
                    value = fetchUsdPrice(currencyCode)
                )
            }
        }
    }

    fun getUsdValue(currencyCode: String, amount: BigDecimal): BigDecimal {
        if (currencyCode == "USD") {
            return amount
        }
        fetchPrice(currencyCode)
        val price = priceCache.getValue(currencyCode).value
        return amount.multiply(price)
    }


    private fun isOlderThanMaxCacheAge(calculatedAtMillis: Long): Boolean {
        return currentTimeMillisFunction() - calculatedAtMillis > maxPriceCacheAgeMs
    }

    private fun fetchUsdPrice(currencyCode: String): BigDecimal {
        logger.frequentInfo { "Fetching price for $currencyCode" }
        val millisBefore = currentTimeMillisFunction()
        val request = Request.Builder()
            .url("$priceApiUrl/prices/USD?currencyCodes=${currencyCode}")
            .get()
            .build()
        val priceResponse = httpClient.newCall(request).execute()
        priceResponse.use {
            metricsService.recordFetchPriceTime(currentTimeMillisFunction() - millisBefore, "currencyCode=$currencyCode,statusCode=${priceResponse.code}")
            check(priceResponse.isSuccessful) { "Could not get price for $currencyCode/USD, code=${priceResponse.code}" }
            val priceDto = objectMapper.readValue(priceResponse.body?.string(), Array<CurrencyPriceDto>::class.java)
            check(priceDto.size == 1) { "No required price in response for $currencyCode" }
            return priceDto.first().price.toBigDecimal()
        }
    }

}
