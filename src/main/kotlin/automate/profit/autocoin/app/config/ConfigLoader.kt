package automate.profit.autocoin.app.config

import automate.profit.autocoin.exchange.ExchangeProvider
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.typesafe.config.ConfigFactory
import java.math.BigDecimal

class ConfigLoader {
    companion object {
        fun loadConfig(profile: String? = System.getProperty("PROFILE", System.getenv("PROFILE"))): AppConfig {
            val defaultConfig = ConfigFactory.load("config/app-base")
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
                    .map { ExchangeProvider().getExchange(it) },
                twoLegArbitrageProfitCacheDuration = config.getDuration("arbitrage.twoLegArbitrageProfitCacheDuration"),
                orderBookUsdAmountThresholds = config.getIntList("arbitrage.orderBookUsdAmountThresholds")
                    .map { BigDecimal(it) },
            )
        }
    }
}
