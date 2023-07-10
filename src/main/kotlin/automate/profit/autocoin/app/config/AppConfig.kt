package automate.profit.autocoin.app.config

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import java.io.File
import java.math.BigDecimal
import java.time.Duration

data class ExchangePair(
    val firstExchange: SupportedExchange,
    val secondExchange: SupportedExchange
)

data class AppConfig(
    // region: app
    val serverPort: Int,
    val serviceName: String,
    val appDataPath: String,
    // endregion

    // region: external services
    val exchangeMediatorApiUrl: String,
    val oauth2ApiUrl: String,
    val oauth2ClientId: String,
    val oauth2ClientSecret: String,
    val telegrafHostname: String,
    // endregion

    val metricsDestination: MetricsDestination,

    // region: arbitrage
    val currencyPairsOverride: Set<CurrencyPair>,
    val exchangesToMonitorOverride: List<SupportedExchange>,
    val twoLegArbitrageProfitCacheDuration: Duration,
    val orderBookUsdAmountThresholds: List<BigDecimal>,
    // endregion
) {
    val metricsFolder: String = appDataPath + File.separator + "metrics"
}
