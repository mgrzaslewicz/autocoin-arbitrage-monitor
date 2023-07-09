package automate.profit.autocoin.app.config

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.SupportedExchange.values
import automate.profit.autocoin.exchange.currency.CurrencyPair
import java.io.File
import java.lang.System.getProperty
import java.lang.System.getenv
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit

data class ExchangePair(
    val firstExchange: SupportedExchange,
    val secondExchange: SupportedExchange
)

data class AppConfig(
    val appServerPort: Int = getPropertyThenEnv("APP_SERVER_PORT", "10021").toInt(),
    val serviceName: String = getPropertyThenEnv("SERVICE_NAME", "autocoin-arbitrage-monitor"),

    val currencyPairsOverride: Set<CurrencyPair> = getPropertyThenEnv(
        "CURRENCY_PAIRS_OVERRIDE",
        { propertyValue ->
            propertyValue.split(",")
                .map { CurrencyPair.of(it) }.toSet()
        },
        emptySet()
    ),
    val exchangesToMonitorOverride: List<SupportedExchange> = getPropertyThenEnv(
        "EXCHANGES_TO_MONITOR_OVERRIDE",
        values().joinToString(",") { it.exchangeName }
    )
        .split(",")
        .map { SupportedExchange.fromExchangeName(it) },


    val exchangeMediatorApiBaseUrl: String = getPropertyThenEnv(
        "EXCHANGE_MEDIATOR_API_URL",
        "http://autocoin-exchange-mediator:9001"
    ),
    val oauth2ApiBaseUrl: String = getPropertyThenEnv("OAUTH2_API_URL", "http://autocoin-auth-service:9002"),

    val oauth2ClientId: String = serviceName,
    val oauth2ClientSecret: String = getPropertyThenEnv("APP_OAUTH_CLIENT_SECRET"),
    val ageOfOldestTwoLegArbitrageProfitToKeepInCacheMs: Long = getPropertyThenEnv(
        "AGE_OF_OLDEST_TWO_LEG_ARBITRAGE_PROFIT_TO_KEEP_IN_CACHE_MS",
        Duration.of(5, ChronoUnit.MINUTES).toMillis().toString()
    ).toLong(),
    val ageOfOldestTwoLegArbitrageProfitToKeepInRepositoryMs: Long = getPropertyThenEnv(
        "AGE_OF_OLDEST_TWO_LEG_ARBITRAGE_PROFIT_TO_KEEP_IN_REPOSITORY_MS",
        Duration.of(24, ChronoUnit.HOURS).toMillis().toString()
    ).toLong(),

    val orderBookUsdAmountThresholds: List<BigDecimal> = getPropertyThenEnv(
        "ORDER_BOOK_USD_AMOUNT_THRESHOLDS",
        "100.0,500.0,1000.0,1500.0,2000.0,3000.0,4000.0,5000.0,6000.0,7000.0,8000.0,9000.0,10000.0,15000.0"
    )
        .split(",")
        .map { BigDecimal(it) },
    val appDataPath: String = getPropertyThenEnv("APP_DATA_PATH", "data"),
    val metricsFolder: String = appDataPath + File.separator + "metrics",
    val telegrafHostname: String = getPropertyThenEnv("TELEGRAF_HOSTNAME", "telegraf"),
    val metricsDestination: MetricsDestination = MetricsDestination.valueOf(
        getPropertyThenEnv(
            "METRICS_DESTINATION",
            MetricsDestination.FILE.name
        )
    ),
)

fun loadConfig(): AppConfig {
    return AppConfig()
}

private fun getPropertyThenEnv(propertyName: String): String {
    return getProperty(propertyName, getenv(propertyName))
}

private fun <T> getPropertyThenEnv(propertyName: String, existingPropertyParser: (String) -> T, defaultValue: T): T {
    val propertyValue = getProperty(propertyName, getenv(propertyName))
    return if (propertyValue != null) {
        existingPropertyParser(propertyValue)
    } else {
        defaultValue
    }
}

private fun getPropertyThenEnv(propertyName: String, defaultValue: String): String {
    return getProperty(propertyName, getenv(propertyName).orElse(defaultValue))
}

private fun String?.orElse(value: String) = this ?: value
