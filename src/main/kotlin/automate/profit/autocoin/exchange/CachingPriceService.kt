package automate.profit.autocoin.exchange

import mu.KLogging
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService

class CachingPriceService(
    private val decorated: PriceService,
    private val maxPriceCacheAgeMs: Long = Duration.of(1, ChronoUnit.HOURS).toMillis(),
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
    private val executorService: ExecutorService,
) : PriceService {
    private companion object : KLogging()

    private val priceCache = ConcurrentHashMap<String, CurrencyPrice>()
    private val currencyLocks = ConcurrentHashMap<String, String>()

    private fun String.currencyLock() = currencyLocks.computeIfAbsent(this) { this }

    override fun getUsdPrice(currencyCode: String): CurrencyPrice {
        synchronized(currencyCode.currencyLock()) {
            if (priceCache.containsKey(currencyCode)) {
                val currencyPrice = priceCache[currencyCode]!!
                if (isOlderThanMaxCacheAge(currencyPrice.timestampMillis)) {
                    submitRefreshPrice(currencyCode)
                }
            }
            priceCache.computeIfAbsent(currencyCode) { decorated.getUsdPrice(currencyCode) }
        }
        return priceCache.getValue(currencyCode)
    }

    private fun submitRefreshPrice(currencyCode: String) {
        executorService.submit {
            try {
                synchronized(currencyCode.currencyLock()) {
                    val currencyPrice = decorated.getUsdPrice(currencyCode)
                    priceCache[currencyCode] = currencyPrice
                }
            } catch (e: Exception) {
                logger.error(e) { "[$currencyCode] Could not refresh price" }
            }
        }
    }

    override fun getUsdValue(currencyCode: String, amount: BigDecimal): BigDecimal {
        val price = getUsdPrice(currencyCode)
        return amount.multiply(price.price)
    }

    private fun isOlderThanMaxCacheAge(calculatedAtMillis: Long): Boolean {
        return currentTimeMillisFunction() - calculatedAtMillis > maxPriceCacheAgeMs
    }
}
