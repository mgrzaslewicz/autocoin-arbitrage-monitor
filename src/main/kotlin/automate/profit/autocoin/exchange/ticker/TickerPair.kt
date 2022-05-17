package automate.profit.autocoin.exchange.ticker

data class TickerPair(
    val first: Ticker,
    val second: Ticker
) {
    fun oldestTickerReceivedAtOrExchangeMillis(): Long {
        return min(first.receivedAtMillis, second.receivedAtMillis, first.exchangeTimestampMillis, second.exchangeTimestampMillis)
    }

    private fun min(long1: Long, long2: Long, long3: Long?, long4: Long?): Long {
        return kotlin.math.min(kotlin.math.min(long1, long2), kotlin.math.min(long3 ?: Long.MAX_VALUE, long4 ?: Long.MAX_VALUE))
    }
}
