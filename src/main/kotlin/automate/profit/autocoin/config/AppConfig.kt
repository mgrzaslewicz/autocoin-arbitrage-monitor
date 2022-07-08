package automate.profit.autocoin.config

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
        CurrencyPair.of("LSK/BTC") to listOf(
                ExchangePair(BITTREX, BINANCE),
                ExchangePair(BITTREX, KUCOIN),
                ExchangePair(KUCOIN, BINANCE)
        ),
        CurrencyPair.of("LSK/ETH") to listOf(
                ExchangePair(KUCOIN, BINANCE)
        ),
        CurrencyPair.of("GNT/ETH") to listOf(
                ExchangePair(BINANCE, BITTREX)
        ),
        CurrencyPair.of("GNT/BTC") to listOf(
                ExchangePair(BINANCE, BITTREX)
        ),
        CurrencyPair.of("EOS/BTC") to listOf(
                ExchangePair(BITTREX, BINANCE),
                ExchangePair(BITTREX, KUCOIN),
                ExchangePair(KUCOIN, BINANCE)
        ),
        CurrencyPair.of("EOS/ETH") to listOf(
                ExchangePair(BITTREX, BINANCE),
                ExchangePair(BITTREX, KUCOIN),
                ExchangePair(KUCOIN, BINANCE)
        ),
        CurrencyPair.of("TRX/BTC") to listOf(
                ExchangePair(BITTREX, BINANCE),
                ExchangePair(BITTREX, KUCOIN),
                ExchangePair(KUCOIN, BINANCE)
        ),
        CurrencyPair.of("TRX/ETH") to listOf(
                ExchangePair(BITTREX, BINANCE),
                ExchangePair(BITTREX, KUCOIN),
                ExchangePair(KUCOIN, BINANCE)
        )
)

data class AppConfig(
        val appServerPort: Int = getPropertyThenEnv("APP_SERVER_PORT", "10021").toInt(),
        val twoLegArbitrageCurrencyAndExchangePairs: Map<CurrencyPair, List<ExchangePair>> = if (getPropertyThenEnv("APP_USE_HARDCODED_TWO_LEG_ARBITRAGE_CURRENCY_AND_EXCHANGE_PAIRS", "false").toBoolean()) currencyPairsForArbitrage else emptyMap(),
        val arbitrageCurrencyPairsWhiteList: Set<CurrencyPair> = getPropertyThenEnv("APP_TWO_LEG_ARBITRAGE_CURRENCY_PAIRS_WHITE_LIST",
                { propertyValue ->
                    propertyValue.split(",")
                            .map { CurrencyPair.of(it) }.toSet()
                },
                emptySet()),
        val exchangeMediatorApiUrl: String = getPropertyThenEnv("EXCHANGE_MEDIATOR_API_URL", "https://orders-api.autocoin-trader.com"),
        val exchangeMetadataServiceHostWithPort: String = getPropertyThenEnv("  EXCHANGE_METADATA_SERVICE_HOST_WITH_PORT", "https://orders-api.autocoin-trader.com"),
        val arbitrageMonitorOauth2ClientId: String = getPropertyThenEnv("APP_OAUTH_CLIENT_ID", "arbitrage-monitor"),
        val arbitrageMonitorOauth2ClientSecret: String = getPropertyThenEnv("APP_OAUTH_CLIENT_SECRET"),
        val oauth2ServerUrl: String = getPropertyThenEnv("OAUTH2_SERVER_URL", "https://users-apiv2.autocoin-trader.com"),
        val tickerPairsRepositoryPath: String = getPropertyThenEnv("APP_DATA_PATH", "data") + File.separator + "tickerPairs",
        val ageOfOldestTwoLegArbitrageProfitToKeepMs: Long = getPropertyThenEnv("APP_AGE_OF_OLDEST_TWO_LEG_ARBITRAGE_PROFIT_TO_KEEP_MS", Duration.of(5, ChronoUnit.MINUTES).toMillis().toString()).toLong(),
        val exchangesToMonitorTwoLegArbitrageOpportunities: List<SupportedExchange> = getPropertyThenEnv("APP_EXCHANGES_TO_MONITOR_TWO_LEG_ARBITRAGE_OPPORTUNITIES", "binance,bittrex,kucoin,bitbay,bitmex,bitstamp,gateio,kraken")
                .split(",")
                .map { SupportedExchange.fromExchangeName(it) },
        val orderBookUsdAmountThresholds: List<BigDecimal> = getPropertyThenEnv("APP_ORDER_BOOK_USD_AMOUNT_THRESHOLDS", "100.0,500.0,1000.0,1500.0")
                .split(",")
                .map { BigDecimal(it) }
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