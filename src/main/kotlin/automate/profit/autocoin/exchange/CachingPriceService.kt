package automate.profit.autocoin.exchange

import mu.KLogging
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.locks.ReentrantLock

class CachingPriceService(
    private val decorated: PriceService,
    private val maxPriceCacheAgeMs: Long = Duration.of(1, ChronoUnit.HOURS).toMillis(),
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
    private val executorService: ExecutorService,
) : PriceService {
    private companion object : KLogging()

    private val priceCache = ConcurrentHashMap<String, CurrencyPrice>()
    private val currencyLocks = ConcurrentHashMap<String, ReentrantLock>()

    override fun getUsdPrice(currencyCode: String): CurrencyPrice {
        if (priceCache.containsKey(currencyCode)) {
            val currencyPrice = priceCache[currencyCode]!!
            if (isOlderThanMaxCacheAge(currencyPrice.timestampMillis)) {
                submitRefreshPrice(currencyCode)
            }
        } else {
            submitRefreshPrice(currencyCode)
        }
        return priceCache.getValue(currencyCode)
    }

    private fun submitRefreshPrice(currencyCode: String) {
        executorService.submit {
            var lock: ReentrantLock? = null
            var isPriceRequested = false
            try {
                synchronized(currencyLocks) {
                    lock = currencyLocks.getOrPut(currencyCode) { ReentrantLock() }
                }
                if (lock!!.tryLock()) {
                    isPriceRequested = true
                    val currencyPrice = decorated.getUsdPrice(currencyCode)
                    priceCache[currencyCode] = currencyPrice
                    lock!!.unlock()
                }
            } catch (e: Exception) {
                logger.error(e) { "[$currencyCode] Could not refresh price" }
            } finally {
                if (isPriceRequested && lock?.isLocked == true) {
                    lock?.unlock()
                }
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
