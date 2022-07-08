package automate.profit.autocoin

import java.lang.System.setProperty
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Copy this file to src/main and provide settings to run
 */
fun main() {
    //    setProperty("OAUTH2_SERVER_URL", "http://localhost:9002")
    setProperty("APP_OAUTH_CLIENT_ID", "changeme")
    setProperty("APP_OAUTH_CLIENT_SECRET", "changeme")
    setProperty("APP_AGE_OF_OLDEST_TICKER_PAIR_TO_KEEP_MS",  Duration.of(5, ChronoUnit.MINUTES).toMillis().toString())
    setProperty("logging.level.automate.profit.autocoin.exchange.ticker", "DEBUG")
    main(emptyArray())
}
