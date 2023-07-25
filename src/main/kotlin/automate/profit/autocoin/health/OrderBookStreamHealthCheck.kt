package automate.profit.autocoin.health

import automate.profit.autocoin.exchange.orderbookstream.OrderBookSseStreamService

class OrderBookStreamHealthCheck(
    private val orderBookSseStreamService: OrderBookSseStreamService,
) : HealthCheck {
    override fun invoke(): HealthCheckResult {
        val isConnected = orderBookSseStreamService.isConnected()
        return HealthCheckResult(
            description = "OrderBook stream",
            healthy = isConnected,
            details = emptyMap(),
            healthCheckClass = this.javaClass,
            unhealthyReasons = listOfNotNull(
                if (!isConnected) "Not connected to order book stream" else null,
            ),
        )
    }
}
