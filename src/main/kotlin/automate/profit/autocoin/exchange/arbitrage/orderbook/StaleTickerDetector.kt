package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerPair
import java.time.Duration
import java.time.temporal.ChronoUnit

class StaleTickerDetector(
    private val currentTimeMillis: () -> Long = System::currentTimeMillis,
    maxAgeOfFirstTicker: Duration = Duration.of(2, ChronoUnit.HOURS),
) {
    private val maxAgeOfFirstTickerMs = maxAgeOfFirstTicker.toMillis()

    fun oneOfTickersIsTooOld(tickerPair: TickerPair): Boolean {
        return oneOfTickersIsTooOld(tickerPair.first, tickerPair.second, currentTimeMillis())
    }

    private fun oneOfTickersIsTooOld(ticker1: Ticker, ticker2: Ticker, currentTimeMillis: Long): Boolean {
        return (currentTimeMillis - (ticker1.timestamp?.toEpochMilli() ?: 0L) > maxAgeOfFirstTickerMs ||
                currentTimeMillis - (ticker2.timestamp?.toEpochMilli() ?: 0L) > maxAgeOfFirstTickerMs)
    }

}