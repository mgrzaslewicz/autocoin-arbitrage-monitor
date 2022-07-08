package automate.profit.autocoin.exchange.metadata

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import mu.KLogging

data class CommonExchangeCurrencyPairs(
        val currencyPairsToExchangePairs: Map<CurrencyPair, Set<ExchangePair>>,
        val exchangePairsToCurrencyPairs: Map<ExchangePair, Set<CurrencyPair>>
)

class CommonExchangeCurrencyPairsService(
        private val exchangeMetadataService: ExchangeMetadataService,
        private val exchanges: List<SupportedExchange>,
        private val currencyPairsWhiteList: Set<CurrencyPair> = emptySet(),
        private val twoLegArbitrageCurrencyAndExchangePairs: Map<CurrencyPair, Set<ExchangePair>> = emptyMap()
) {

    companion object : KLogging()

    fun calculateCommonCurrencyPairs(): CommonExchangeCurrencyPairs {
        if (twoLegArbitrageCurrencyAndExchangePairs.isNotEmpty()) {
            logger.warn { "Using hardcoded currency pairs for monitoring profits" }
            return CommonExchangeCurrencyPairs(
                    currencyPairsToExchangePairs = twoLegArbitrageCurrencyAndExchangePairs,
                    exchangePairsToCurrencyPairs = emptyMap()
            )
        }
        val currencyPairsToExchangePairs: MutableMap<CurrencyPair, MutableSet<ExchangePair>> = mutableMapOf()
        val exchangePairsToCurrencyPairs: MutableMap<ExchangePair, Set<CurrencyPair>> = mutableMapOf()
        val exchangesWithCurrencyPairs = fetchExchangesWithCurrencyPairs()
        for (i in exchangesWithCurrencyPairs.indices) {
            for (j in i + 1 until exchangesWithCurrencyPairs.size) {
                val exchangePair = ExchangePair(SupportedExchange.fromExchangeName(exchangesWithCurrencyPairs[i].first), SupportedExchange.fromExchangeName(exchangesWithCurrencyPairs[j].first))
                val commonCurrencyPairs = findCommonCurrencyPairs(exchangesWithCurrencyPairs[i].second, exchangesWithCurrencyPairs[j].second)
                exchangePairsToCurrencyPairs[exchangePair] = commonCurrencyPairs
                commonCurrencyPairs.forEach {
                    currencyPairsToExchangePairs.putIfAbsent(it, mutableSetOf())
                    currencyPairsToExchangePairs[it]!!.add(exchangePair)
                }
            }
        }
        return CommonExchangeCurrencyPairs(
                currencyPairsToExchangePairs = currencyPairsToExchangePairs,
                exchangePairsToCurrencyPairs = exchangePairsToCurrencyPairs
        )
    }

    private fun findCommonCurrencyPairs(firstExchangeCurrencyPairs: Set<CurrencyPair>, secondExchangeCurrencyPairs: Set<CurrencyPair>): Set<CurrencyPair> {
        val currencyPairs = firstExchangeCurrencyPairs
                .filter { it in secondExchangeCurrencyPairs }.toSet()
        return if (currencyPairsWhiteList.isEmpty()) {
            currencyPairs
        } else {
            currencyPairs.filter { it in currencyPairsWhiteList }.toSet()
        }
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