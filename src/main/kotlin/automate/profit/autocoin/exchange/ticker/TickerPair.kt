package automate.profit.autocoin.exchange.ticker

import com.autocoin.exchangegateway.spi.exchange.ticker.Ticker

data class TickerPair(
    val first: Ticker?,
    val second: Ticker?,
) {
    fun oldestTickerReceivedAtOrExchangeMillis(): Long? {
        return when {
            first != null && second != null -> {
                min(first.receivedAtMillis, second.receivedAtMillis, first.exchangeTimestampMillis, second.exchangeTimestampMillis)
            }
            first != null && second == null -> {
                Math.min(first.receivedAtMillis, first.exchangeTimestampMillis ?: Long.MAX_VALUE)
            }
            first == null && second != null -> {
                Math.min(second.receivedAtMillis, second.exchangeTimestampMillis ?: Long.MAX_VALUE)
            }
            else -> null
        }
    }

    private fun min(long1: Long, long2: Long, long3: Long?, long4: Long?): Long {
        return kotlin.math.min(kotlin.math.min(long1, long2), kotlin.math.min(long3 ?: Long.MAX_VALUE, long4 ?: Long.MAX_VALUE))
    }
}
