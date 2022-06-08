package automate.profit.autocoin

import java.lang.System.setProperty

/**
 * Copy this file to src/main and provide settings to run
 * Add limits when running process
-Xmx400M
-XX:+ExitOnOutOfMemoryError
-XX:+HeapDumpOnOutOfMemoryError
 */
fun main() {
//     setProperty("OAUTH2_API_URL", "http://localhost:9002")
//    setProperty("EXCHANGE_MEDIATOR_API_URL", "http://localhost:9001")
    setProperty("SERVICE_NAME", "autocoin-arbitrage-service")
    setProperty("APP_OAUTH_CLIENT_ID", "changeme")
    setProperty("APP_OAUTH_CLIENT_SECRET", "changeme")
//    setProperty("APP_USE_HARDCODED_TWO_LEG_ARBITRAGE_CURRENCY_AND_EXCHANGE_PAIRS", "true")
    setProperty("APP_EXCHANGES_TO_MONITOR_TWO_LEG_ARBITRAGE_OPPORTUNITIES", "bibex,binance,bittrex")
//    setProperty("APP_TWO_LEG_ARBITRAGE_CURRENCY_PAIRS_WHITE_LIST", "QTUM/BTC,GAS/BTC")
    setProperty("USE_METRICS", "false")
    setProperty("TELEGRAF_HOSTNAME", "localhost")
    setProperty("logging.level.automate.profit.autocoin.exchange.ticker", "DEBUG")
    setProperty("logging.level.automate.profit.autocoin.exchange.orderbook", "DEBUG")
    setProperty("logging.level.automate.profit.autocoin.exchange.ticker.TickerPairCache", "DEBUG")
    setProperty("logging.level", "DEBUG")
    main(emptyArray())
}
