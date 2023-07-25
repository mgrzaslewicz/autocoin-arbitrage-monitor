package automate.profit.autocoin.exchange.metadata

import java.util.concurrent.ConcurrentHashMap

class CachingExchangeMetadataService(private val decorated: ExchangeMetadataService) : ExchangeMetadataService {
    private val locks = ConcurrentHashMap<String, Any>()
    private val cachePerExchange = ConcurrentHashMap<String, ExchangeMetadata>()
    private val allExchangesCache: List<ExchangeMetadata> by lazy { decorated.getAllExchangesMetadata() }

    override fun getAllExchangesMetadata() = allExchangesCache

    override fun getMetadata(exchangeName: String): ExchangeMetadata {
        synchronized(locks.computeIfAbsent(exchangeName) { Any() }) {
            return cachePerExchange.computeIfAbsent(exchangeName) { decorated.getMetadata(exchangeName) }
        }
    }

}

fun ExchangeMetadataService.caching(): ExchangeMetadataService = CachingExchangeMetadataService(this)
