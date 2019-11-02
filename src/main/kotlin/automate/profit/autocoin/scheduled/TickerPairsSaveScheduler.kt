package automate.profit.autocoin.scheduled

import automate.profit.autocoin.exchange.ticker.FileTickerPairRepository
import automate.profit.autocoin.exchange.ticker.TickerPairCache
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

class TickerPairsSaveScheduler(private val tickerPairCache: TickerPairCache, private val tickerPairRepository: FileTickerPairRepository) {
    private val executorService = Executors.newScheduledThreadPool(1)

    fun scheduleSavingTickerPairs() {
        executorService.scheduleAtFixedRate({
            tickerPairCache.getCurrencyPairWithExchangePairs().forEach {
                tickerPairRepository.saveAll(it.currencyPair, it.exchangePair, tickerPairCache.getTickerCurrencyPairs(it))
                tickerPairRepository.removeAllButLatestTickerPairFile(it.currencyPair, it.exchangePair)
            }
        }, 0, 30, TimeUnit.SECONDS)
    }

}