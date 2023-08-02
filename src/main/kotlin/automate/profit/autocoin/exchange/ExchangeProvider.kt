package automate.profit.autocoin.exchange

import com.autocoin.exchangegateway.spi.exchange.Exchange
import java.util.concurrent.ConcurrentHashMap
import com.autocoin.exchangegateway.spi.exchange.ExchangeProvider as SpiExchangeProvider

class ExchangeProvider : SpiExchangeProvider {
    override fun getExchange(exchangeName: String): Exchange {
        return object : Exchange {
            override val exchangeName = exchangeName
        }
    }
}

class CachingExchangeProvider(private val decorated: SpiExchangeProvider) : SpiExchangeProvider {
    private val cache = ConcurrentHashMap<String, Exchange>()

    override fun getExchange(exchangeName: String): Exchange {
        return cache.computeIfAbsent(exchangeName) { decorated.getExchange(exchangeName) }
    }
}

fun SpiExchangeProvider.caching(): SpiExchangeProvider = CachingExchangeProvider(this)
