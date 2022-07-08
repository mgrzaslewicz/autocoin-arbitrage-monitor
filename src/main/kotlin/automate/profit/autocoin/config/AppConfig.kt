package automate.profit.autocoin.config

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import java.lang.System.getProperty

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
        val oauth2ServerUrl: String
)

fun loadConfig(): AppConfig {
    return AppConfig(
            appServerPort = getProperty("APP_SERVER_PORT", "10021").toInt(),
            tickerApiUrl = getProperty("TICKER_API_URL", "https://orders-api.autocoin-trader.com"),
            arbitrageMonitorOauth2ClientId = getProperty("APP_OAUTH_CLIENT_ID", "arbitrage-monitor"),
            arbitrageMonitorOauth2ClientSecret = getProperty("APP_OAUTH_CLIENT_SECRET"),
            oauth2ServerUrl = getProperty("OAUTH2_SERVER_URL", "https://users-apiv2.autocoin-trader.com")
    )
}