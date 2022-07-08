package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.ticker.FileTickerPairRepository
import automate.profit.autocoin.exchange.ticker.TickerPairCache
import mu.KLogging
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TickerPairsSaveScheduler(private val tickerPairCache: TickerPairCache, private val tickerPairRepository: FileTickerPairRepository) {
    companion object : KLogging()

    private val executorService = Executors.newScheduledThreadPool(1)

    fun scheduleSavingTickerPairs() {
        logger.info { "Will save ticker pairs every 30 seconds" }
        executorService.scheduleAtFixedRate({
            tickerPairCache.getCurrencyPairWithExchangePairs().forEach {
                tickerPairRepository.saveAll(it, tickerPairCache.getTickerCurrencyPairs(it))
                tickerPairRepository.removeAllButLatestTickerPairFile(it)
            }
        }, 0, 30, TimeUnit.SECONDS)
    }

}