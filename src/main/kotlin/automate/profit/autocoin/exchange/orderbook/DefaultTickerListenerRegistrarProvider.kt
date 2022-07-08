package automate.profit.autocoin.exchange.orderbook

import automate.profit.autocoin.exchange.SupportedExchange
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.OkHttpClient
import java.util.concurrent.Executors

class DefaultOrderBookListenerRegistrarProvider(
        private val orderBookApiUrl: String,
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper
) : OrderBookListenerRegistrarProvider {
    override fun createOrderBookListenerRegistrar(exchangeName: SupportedExchange): OrderBookListenerRegistrar {
        return DefaultOrderBookListenerRegistrar(
                exchangeName = exchangeName,
                userExchangeOrderBookService = OrderBookFetcher(supportedExchange = exchangeName, orderBookApiUrl = orderBookApiUrl, httpClient = httpClient, objectMapper = objectMapper),
                executorService = Executors.newWorkStealingPool()
        )
    }
}