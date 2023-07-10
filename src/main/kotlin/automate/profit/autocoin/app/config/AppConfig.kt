package automate.profit.autocoin.app.config

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.typesafe.config.ConfigFactory
import java.io.File
import java.lang.System.getProperty
import java.lang.System.getenv
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

fun loadConfig(): AppConfig {
    val defaultConfig = ConfigFactory.load("config/app-base")
    val profile = getPropertyThenEnv("PROFILE")
    val profileConfig = if (profile == null) {
        ConfigFactory.empty()
    } else {
        ConfigFactory.parseResources("config/app-$profile.conf")
    }
    val config = ConfigFactory
        .systemEnvironment()
        .withFallback(ConfigFactory.systemProperties())
        .withFallback(profileConfig)
        .withFallback(defaultConfig)
        .resolve()

    return AppConfig(
        serverPort = config.getInt("server.port"),
        serviceName = config.getString("service.name"),

        oauth2ApiUrl = config.getString("externalServices.oauth.apiUrl"),
        oauth2ClientId = config.getString("externalServices.oauth.clientId"),
        oauth2ClientSecret = config.getString("externalServices.oauth.clientSecret"),
        exchangeMediatorApiUrl = config.getString("externalServices.exchangeMediator.apiUrl"),
        telegrafHostname = config.getString("externalServices.telegrafHostname"),

        appDataPath = config.getString("service.dataFolder"),

        metricsDestination = MetricsDestination.valueOf(config.getString("metrics.destination")),

        currencyPairsOverride = config.getStringList("arbitrage.currencyPairsOverride")
            .map { CurrencyPair.of(it) }
            .toSet(),
        exchangesToMonitorOverride = config.getStringList("arbitrage.exchangesToMonitorOverride")
            .map { SupportedExchange.fromExchangeName(it) },
        twoLegArbitrageProfitCacheDuration = config.getDuration("arbitrage.ageOfOldestTwoLegArbitrageProfitToKeepInCache"),
        orderBookUsdAmountThresholds = config.getIntList("arbitrage.orderBookUsdAmountThresholds")
            .map { BigDecimal(it) },
    )
}

private fun getPropertyThenEnv(propertyName: String): String? {
    return getProperty(propertyName, getenv(propertyName))
}
