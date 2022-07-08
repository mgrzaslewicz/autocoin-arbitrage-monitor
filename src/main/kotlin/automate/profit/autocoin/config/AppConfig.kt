package automate.profit.autocoin.config

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import java.io.File
import java.lang.System.getProperty
import java.time.Duration
import java.time.temporal.ChronoUnit

data class ExchangePair(
        val firstExchange: SupportedExchange,
        val secondExchange: SupportedExchange
)

private val commonCurrencyPairs = listOf(
        CurrencyPair.of("XRP/BTC"),
        CurrencyPair.of("ETH/BTC"),
        CurrencyPair.of("LSK/BTC")

)
private val exchangePairsForArbitrage = listOf(
        ExchangePair(BITTREX, BINANCE),
        ExchangePair(BINANCE, KUCOIN),
        ExchangePair(BITTREX, KUCOIN)
)

data class AppConfig(
        val appServerPort: Int,
        val twoLegArbitragePairs: Map<CurrencyPair, List<ExchangePair>> = commonCurrencyPairs.map { it to exchangePairsForArbitrage }.toMap(),
        val tickerApiUrl: String,
        val arbitrageMonitorOauth2ClientId: String,
        val arbitrageMonitorOauth2ClientSecret: String,
        val oauth2ServerUrl: String,
        val tickerPairsRepositoryPath: String,
        val ageOfOldestTickerPairToKeepMs: Long
)

fun loadConfig(): AppConfig {
    return AppConfig(
            appServerPort = getProperty("APP_SERVER_PORT", "10021").toInt(),
            tickerApiUrl = getProperty("TICKER_API_URL", "https://orders-api.autocoin-trader.com"),
            arbitrageMonitorOauth2ClientId = getProperty("APP_OAUTH_CLIENT_ID", "arbitrage-monitor"),
            arbitrageMonitorOauth2ClientSecret = getProperty("APP_OAUTH_CLIENT_SECRET"),
            oauth2ServerUrl = getProperty("OAUTH2_SERVER_URL", "https://users-apiv2.autocoin-trader.com"),
            tickerPairsRepositoryPath = getProperty("APP_DATA_PATH", "data") + File.separator + "tickerPairs",
            ageOfOldestTickerPairToKeepMs = getProperty("APP_AGE_OF_OLDEST_TICKER_PAIR_TO_KEEP_MS", Duration.of(24, ChronoUnit.HOURS).toMillis().toString()).toLong()
    )
}