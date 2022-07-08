package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.ticker.FileTickerPairRepository
import automate.profit.autocoin.exchange.ticker.TickerPairCache
import mu.KLogging
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class TickerPairsSaveScheduler(
        private val tickerPairCache: TickerPairCache,
        private val tickerPairRepository: FileTickerPairRepository,
        private val executorService: ScheduledExecutorService
) {
    companion object : KLogging()

    fun scheduleSavingTickerPairs() {
        logger.info { "Will save ticker pairs every 30 seconds" }
        executorService.scheduleAtFixedRate({
            try {
                tickerPairCache.getCurrencyPairWithExchangePairs().forEach {
                    val tickerPairs = tickerPairCache.getAndCleanTickerCurrencyPairs(it)
                    tickerPairRepository.addAll(it, tickerPairs)
                    tickerPairRepository.removeTooOldTickers(it)
                    tickerPairRepository.removeAllButLatestTickerPairFile(it)
                }
            } catch (e: Exception) {
                logger.error(e) { "Could not save ticker pairs" }
            }
        }, 0, 30, TimeUnit.SECONDS)
    }

}