package automate.profit.autocoin.exchange.metadata

import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.metadata.ExchangeMetadata
import com.autocoin.exchangegateway.spi.exchange.metadata.gateway.AuthorizedMetadataServiceGateway
import java.util.*
import java.util.concurrent.ConcurrentHashMap

class CachingExchangeMetadataServiceGateway(private val decorated: AuthorizedMetadataServiceGateway) :
    AuthorizedMetadataServiceGateway {
    private val locks = ConcurrentHashMap<Exchange, Any>()
    private val cachePerExchange = ConcurrentHashMap<Exchange, ExchangeMetadata>()

    override fun getMetadata(exchange: Exchange): ExchangeMetadata {
        synchronized(locks.computeIfAbsent(exchange) { Any() }) {
            return cachePerExchange.computeIfAbsent(exchange) { decorated.getMetadata(exchange) }
        }
    }

    override fun getAllExchangesMetadata(): Map<Exchange, ExchangeMetadata> {
        decorated.getAllExchangesMetadata().forEach { (exchange, metadata) ->
            synchronized(locks.computeIfAbsent(exchange) { Any() }) {
                cachePerExchange[exchange] = metadata
            }
        }
        return Collections.unmodifiableMap(cachePerExchange)
    }
}

fun AuthorizedMetadataServiceGateway.caching(): AuthorizedMetadataServiceGateway =
    CachingExchangeMetadataServiceGateway(this)
