package automate.profit.autocoin.exchange.metadata

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogging

class CommonExchangeCurrencyPairsService(
        private val exchangeMetadataService: ExchangeMetadataService,
        private val exchanges: List<SupportedExchange> = listOf(BINANCE, BITTREX),//, KUCOIN, BITBAY, BITMEX, BITSTAMP, GATEIO, KRAKEN),
        private val twoLegArbitragePairs: Map<CurrencyPair, List<ExchangePair>> = emptyMap()
) {
    private var cachedResult: MutableMap<CurrencyPair, MutableList<ExchangePair>>? = null

    companion object : KLogging()

    fun getCommonCurrencyPairs(): Map<CurrencyPair, List<ExchangePair>> {
        if (cachedResult != null) {
            return cachedResult!!
        }
        if (twoLegArbitragePairs.isNotEmpty()) {
            logger.warn { "Using hardcoded currency pairs for monitoring profits" }
            return twoLegArbitragePairs
        }
        cachedResult = mutableMapOf<CurrencyPair, MutableList<ExchangePair>>()
        val exchangesWithCurrencyPairs = fetchExchangesWithCurrencyPairs()
        for (i in exchangesWithCurrencyPairs.indices) {
            for (j in i + 1 until exchangesWithCurrencyPairs.size) {
                val exchangePair = ExchangePair(SupportedExchange.fromExchangeName(exchangesWithCurrencyPairs[i].first), SupportedExchange.fromExchangeName(exchangesWithCurrencyPairs[j].first))
                val commonCurrencyPairs = findCommonCurrencyPairs(exchangesWithCurrencyPairs[i].second, exchangesWithCurrencyPairs[j].second)
                commonCurrencyPairs.forEach {
                    cachedResult!!.putIfAbsent(it, mutableListOf())
                    cachedResult!![it]!!.add(exchangePair)
                }
            }
        }
        return cachedResult!!
    }

    private fun findCommonCurrencyPairs(firstExchangeCurrencyPairs: Set<CurrencyPair>, secondExchangeCurrencyPairs: Set<CurrencyPair>): Set<CurrencyPair> {
        return firstExchangeCurrencyPairs.filter { secondExchangeCurrencyPairs.contains(it) }.toSet()
    }

    private fun fetchExchangesWithCurrencyPairs(): Array<Pair<String, Set<CurrencyPair>>> {
        val metadataList = mutableListOf<Pair<String, ExchangeMetadata>>()
        runBlocking {
            exchanges.map {
                launch {
                    metadataList.add(Pair(it.exchangeName, exchangeMetadataService.getMetadata(it.exchangeName)))
                }
            }.forEach { it.join() }
        }
        return metadataList.map { it.first to it.second.currencyPairMetadata.keys }.toTypedArray()
    }
}