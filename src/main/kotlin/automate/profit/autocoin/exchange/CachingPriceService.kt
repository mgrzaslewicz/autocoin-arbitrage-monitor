package automate.profit.autocoin.exchange

import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap

class CachingPriceService(
    private val decorated: PriceService,
    private val maxPriceCacheAgeMs: Long = Duration.of(1, ChronoUnit.HOURS).toMillis(),
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
) : PriceService {

    private data class ValueWithTimestamp(
        val value: BigDecimal,
        val calculatedAtMillis: Long
    )

    private val priceCache = ConcurrentHashMap<String, ValueWithTimestamp>()
    private val currencyLocks = ConcurrentHashMap<String, String>()

    override fun getUsdPrice(currencyCode: String): BigDecimal {
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
                    value = decorated.getUsdPrice(currencyCode)
                )
            }
        }
        return priceCache.getValue(currencyCode).value
    }

    override fun getUsdValue(currencyCode: String, amount: BigDecimal): BigDecimal {
        val price = getUsdPrice(currencyCode)
        return amount.multiply(price)
    }

    private fun isOlderThanMaxCacheAge(calculatedAtMillis: Long): Boolean {
        return currentTimeMillisFunction() - calculatedAtMillis > maxPriceCacheAgeMs
    }
}
