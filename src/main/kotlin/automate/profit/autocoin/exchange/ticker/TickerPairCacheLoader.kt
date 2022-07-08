package automate.profit.autocoin.exchange.ticker

import mu.KLogging

class TickerPairCacheLoader(private val tickerPairCache: TickerPairCache, private val tickerPairRepository: FileTickerPairRepository) {

    companion object : KLogging()

    fun loadAllSavedTickerPairs() {
        tickerPairRepository.getAllCurrencyPairsWithExchangePairs().forEach { currencyPairWithExchangePair ->
            logger.info { "Loading $currencyPairWithExchangePair" }
            tickerPairRepository.getTickerPairs(currencyPairWithExchangePair).forEach { tickerPair ->
                tickerPairCache.addTickerPair(currencyPairWithExchangePair, tickerPair)
            }
        }
    }

}