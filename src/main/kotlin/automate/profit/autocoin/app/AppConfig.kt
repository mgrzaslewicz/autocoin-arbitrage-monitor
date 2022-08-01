package automate.profit.autocoin.app

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.SupportedExchange.*
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

private val currencyPairsForArbitrage = mapOf(
    CurrencyPair.of("LSK/BTC") to setOf(
        ExchangePair(BITTREX, BINANCE),
        ExchangePair(BITTREX, KUCOIN),
        ExchangePair(KUCOIN, BINANCE)
    ),
    CurrencyPair.of("LSK/ETH") to setOf(
        ExchangePair(KUCOIN, BINANCE)
    ),
    CurrencyPair.of("GNT/ETH") to setOf(
        ExchangePair(BINANCE, BITTREX)
    ),
    CurrencyPair.of("GNT/BTC") to setOf(
        ExchangePair(BINANCE, BITTREX)
    ),
    CurrencyPair.of("EOS/BTC") to setOf(
        ExchangePair(BITTREX, BINANCE),
        ExchangePair(BITTREX, KUCOIN),
        ExchangePair(KUCOIN, BINANCE)
    ),
    CurrencyPair.of("EOS/ETH") to setOf(
        ExchangePair(BITTREX, BINANCE),
        ExchangePair(BITTREX, KUCOIN),
        ExchangePair(KUCOIN, BINANCE)
    ),
    CurrencyPair.of("TRX/BTC") to setOf(
        ExchangePair(BITTREX, BINANCE),
        ExchangePair(BITTREX, KUCOIN),
        ExchangePair(KUCOIN, BINANCE)
    ),
    CurrencyPair.of("TRX/ETH") to setOf(
        ExchangePair(BITTREX, BINANCE),
        ExchangePair(BITTREX, KUCOIN),
        ExchangePair(KUCOIN, BINANCE)
    )
)

data class AppConfig(
    val appServerPort: Int = getPropertyThenEnv("APP_SERVER_PORT", "10021").toInt(),
    val serviceName: String = getPropertyThenEnv("SERVICE_NAME", "autocoin-arbitrage-monitor"),

    val twoLegArbitrageCurrencyAndExchangePairs: Map<CurrencyPair, Set<ExchangePair>> = if (getPropertyThenEnv(
            "APP_USE_HARDCODED_TWO_LEG_ARBITRAGE_CURRENCY_AND_EXCHANGE_PAIRS",
            "false"
        ).toBoolean()
    ) currencyPairsForArbitrage else emptyMap(),
    val arbitrageCurrencyPairsWhiteList: Set<CurrencyPair> = getPropertyThenEnv(
        "APP_TWO_LEG_ARBITRAGE_CURRENCY_PAIRS_WHITE_LIST",
        { propertyValue ->
            propertyValue.split(",")
                .map { CurrencyPair.of(it) }.toSet()
        },
        emptySet()
    ),
    val exchangesToMonitorTwoLegArbitrageOpportunities: List<SupportedExchange> = getPropertyThenEnv(
        "APP_EXCHANGES_TO_MONITOR_TWO_LEG_ARBITRAGE_OPPORTUNITIES",
        values().joinToString(",") { it.exchangeName }
    )
        .split(",")
        .map { SupportedExchange.fromExchangeName(it) },


    val exchangeMediatorApiBaseUrl: String = getPropertyThenEnv("EXCHANGE_MEDIATOR_API_URL", "http://autocoin-exchange-mediator:9001"),
    val exchangeMetadataApiBaseUrl: String = getPropertyThenEnv("EXCHANGE_METADATA_API_URL", "http://autocoin-exchange-mediator:9001"),
    val oauth2ApiBaseUrl: String = getPropertyThenEnv("OAUTH2_API_URL", "http://autocoin-auth-service:9002"),

    val arbitrageMonitorOauth2ClientId: String = serviceName,
    val arbitrageMonitorOauth2ClientSecret: String = getPropertyThenEnv("APP_OAUTH_CLIENT_SECRET"),
    val metricsFolder: String = getPropertyThenEnv("APP_DATA_PATH", "data") + File.separator + "metrics",
    val ageOfOldestTwoLegArbitrageProfitToKeepInCacheMs: Long = getPropertyThenEnv(
        "APP_AGE_OF_OLDEST_TWO_LEG_ARBITRAGE_PROFIT_TO_KEEP_IN_CACHE_MS",
        Duration.of(5, ChronoUnit.MINUTES).toMillis().toString()
    ).toLong(),
    val ageOfOldestTwoLegArbitrageProfitToKeepInRepositoryMs: Long = getPropertyThenEnv(
        "APP_AGE_OF_OLDEST_TWO_LEG_ARBITRAGE_PROFIT_TO_KEEP_IN_REPOSITORY_MS",
        Duration.of(24, ChronoUnit.HOURS).toMillis().toString()
    ).toLong(),

    val orderBookUsdAmountThresholds: List<BigDecimal> = getPropertyThenEnv(
        "APP_ORDER_BOOK_USD_AMOUNT_THRESHOLDS",
        "100.0,500.0,1000.0,1500.0,2000.0,3000.0,4000.0,5000.0,6000.0,7000.0,8000.0,9000.0,10000.0,15000.0"
    )
        .split(",")
        .map { BigDecimal(it) },
    val profitsRepositoryPath: String = getPropertyThenEnv("APP_DATA_PATH", "data") + File.separator + "profits",
    val telegrafHostname: String = getPropertyThenEnv("TELEGRAF_HOSTNAME", "telegraf"),
    val useRealStatsDClient: Boolean = telegrafHostname != "metrics.jsonl",
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
