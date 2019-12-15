package automate.profit.autocoin

import java.lang.System.setProperty
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * Copy this file to src/main and provide settings to run
 */
fun main() {
//     setProperty("OAUTH2_SERVER_URL", "http://localhost:9002")
//    setProperty("EXCHANGE_MEDIATOR_API_URL", "http://localhost:9001")
    setProperty("APP_OAUTH_CLIENT_ID", "changeme")
    setProperty("APP_OAUTH_CLIENT_SECRET", "changeme")
    setProperty("APP_AGE_OF_OLDEST_TICKER_PAIR_TO_KEEP_IN_REPOSITORY_MS", Duration.of(5, ChronoUnit.MINUTES).toMillis().toString())
//    setProperty("APP_USE_HARDCODED_TWO_LEG_ARBITRAGE_CURRENCY_AND_EXCHANGE_PAIRS", "true")
    setProperty("APP_EXCHANGES_TO_MONITOR_TWO_LEG_ARBITRAGE_OPPORTUNITIES", "binance,bittrex,kucoin")
//    setProperty("APP_TWO_LEG_ARBITRAGE_CURRENCY_PAIRS_WHITE_LIST", "QTUM/BTC,GAS/BTC")
    setProperty("logging.level.automate.profit.autocoin.exchange.ticker", "DEBUG")
    setProperty("logging.level.automate.profit.autocoin.exchange.orderbook", "DEBUG")
    setProperty("logging.level.automate.profit.autocoin.exchange.ticker.TickerPairCache", "DEBUG")
    main(emptyArray())
}
