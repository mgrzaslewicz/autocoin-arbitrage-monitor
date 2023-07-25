package automate.profit.autocoin.health

data class HealthCheckResult(
    val description: String,
    val healthy: Boolean,
    val details: Map<String, String>,
    val healthCheckClass: Class<HealthCheck>,
    val unhealthyReasons: List<String>,
)


interface HealthCheck {
    operator fun invoke(): HealthCheckResult
}
