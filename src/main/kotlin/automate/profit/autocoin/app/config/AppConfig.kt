package automate.profit.autocoin.app.config

import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.Exchange
import java.io.File
import java.math.BigDecimal
import java.time.Duration

data class ExchangePair(
    val firstExchange: Exchange,
    val secondExchange: Exchange
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
    val exchangesToMonitorOverride: List<Exchange>,
    val twoLegArbitrageProfitCacheDuration: Duration,
    val orderBookUsdAmountThresholds: List<BigDecimal>,
    // endregion
) {
    val metricsFolder: String = appDataPath + File.separator + "metrics"
}
