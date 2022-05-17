package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.ticker.TickerPair
import java.time.Duration
import java.time.temporal.ChronoUnit

class StaleTickerDetector(
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
    maxTickerAge: Duration = Duration.of(15, ChronoUnit.MINUTES),
) {
    private val maxTickerAgeMillis = maxTickerAge.toMillis()

    fun oneOfTickersIsTooOld(tickerPair: TickerPair): Boolean {
        val currentTimeMillis = currentTimeMillisFunction()
        return currentTimeMillis - tickerPair.oldestTickerReceivedAtOrExchangeMillis() > maxTickerAgeMillis
    }

}
