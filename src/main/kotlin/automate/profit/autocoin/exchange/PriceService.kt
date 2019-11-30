package automate.profit.autocoin.exchange

import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReadWriteLock
import java.util.concurrent.locks.ReentrantReadWriteLock


data class PriceDto(
        val currency: String,
        val price: Double,
        val unitCurrency: String
)

class PriceService(private val priceApiUrl: String,
                   private val httpClient: OkHttpClient,
                   private val objectMapper: ObjectMapper,
                   private val maxPriceCacheAge: Long = Duration.of(12, ChronoUnit.HOURS).toMillis(),
                   private val currentTimeMillis: () -> Long = System::currentTimeMillis) {

    private data class ValueWithTimestamp(
            val value: BigDecimal,
            val calculatedAtMillis: Long
    )

    private val priceCache = ConcurrentHashMap<String, ValueWithTimestamp>()

    companion object : KLogging()

    private val currencyCodeLocks = ConcurrentHashMap<String, ReadWriteLock>()

    fun getUsdValue(currencyCode: String, amount: BigDecimal): BigDecimal {
        if (currencyCode == "USD") {
            return amount
        }
        val lock = currencyCodeLocks.computeIfAbsent(currencyCode) {
            ReentrantReadWriteLock()
        }

        lock.writeLock().lock()
        if (priceCache.containsKey(currencyCode)) {
            val valueWithTimestamp = priceCache[currencyCode]!!
            if (isOlderThanMaxCacheAge(valueWithTimestamp.calculatedAtMillis)) {
                priceCache.remove(currencyCode)
            }
        }

        priceCache.computeIfAbsent(currencyCode) {
            ValueWithTimestamp(
                    calculatedAtMillis = currentTimeMillis(),
                    value = fetchUsdPrice(currencyCode)
            )
        }
        lock.writeLock().unlock()

        val price = priceCache.getValue(currencyCode).value

        return amount.multiply(price)
    }


    private fun isOlderThanMaxCacheAge(calculatedAtMillis: Long): Boolean {
        return currentTimeMillis() - calculatedAtMillis > maxPriceCacheAge
    }

    private fun fetchUsdPrice(currencyCode: String): BigDecimal {
        logger.info { "Fetching price for $currencyCode" }
        val request = Request.Builder()
                .url("$priceApiUrl/prices/USD?currencyCodes=${currencyCode}")
                .get()
                .build()
        val priceResponse = httpClient.newCall(request).execute()
        priceResponse.use {
            check(priceResponse.code == 200) { "Could not get price for $currencyCode, code=${priceResponse.code}" }
            val priceDto = objectMapper.readValue(priceResponse.body?.string(), Array<PriceDto>::class.java)
            check(priceDto.size == 1) { "No required price in response for $currencyCode" }
            return priceDto.first().price.toBigDecimal()
        }
    }
}