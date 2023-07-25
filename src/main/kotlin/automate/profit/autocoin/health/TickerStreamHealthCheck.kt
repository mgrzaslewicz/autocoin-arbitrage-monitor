package automate.profit.autocoin.health

import automate.profit.autocoin.exchange.tickerstream.TickerSseStreamService

class TickerStreamHealthCheck(
    private val tickerSseStreamService: TickerSseStreamService,
) : HealthCheck {
    override fun invoke(): HealthCheckResult {
        val isConnected = tickerSseStreamService.isConnected()
        return HealthCheckResult(
            description = "Ticker stream",
            healthy = isConnected,
            details = emptyMap(),
            healthCheckClass = this.javaClass,
            unhealthyReasons = listOfNotNull(
                if (!isConnected) "Not connected to ticker stream" else null,
            ),
        )
    }
}
