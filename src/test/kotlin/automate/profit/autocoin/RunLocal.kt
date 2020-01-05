package automate.profit.autocoin

import java.lang.System.setProperty
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Copy this file to src/main and provide settings to run
 * Add limits when running process
-Xmx200M
-XX:+ExitOnOutOfMemoryError
 */
fun main() {
//     setProperty("OAUTH2_SERVER_URL", "http://localhost:9002")
//    setProperty("EXCHANGE_MEDIATOR_API_URL", "http://localhost:9001")
    setProperty("APP_OAUTH_CLIENT_ID", "changeme")
    setProperty("APP_OAUTH_CLIENT_SECRET", "changeme")
    setProperty("APP_SAVE_METRICS_TO_FILE_EVERY_N_SECONDS", "60")
//    setProperty("APP_USE_HARDCODED_TWO_LEG_ARBITRAGE_CURRENCY_AND_EXCHANGE_PAIRS", "true")
    setProperty("APP_EXCHANGES_TO_MONITOR_TWO_LEG_ARBITRAGE_OPPORTUNITIES", "bibex,binance,bittrex")
//    setProperty("APP_TWO_LEG_ARBITRAGE_CURRENCY_PAIRS_WHITE_LIST", "QTUM/BTC,GAS/BTC")
    setProperty("logging.level.automate.profit.autocoin.exchange.ticker", "DEBUG")
    setProperty("logging.level.automate.profit.autocoin.exchange.orderbook", "DEBUG")
    setProperty("logging.level.automate.profit.autocoin.exchange.ticker.TickerPairCache", "DEBUG")
    setProperty("logging.level", "DEBUG")
    main(emptyArray())
}
