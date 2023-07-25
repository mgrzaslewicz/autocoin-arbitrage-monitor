package automate.profit.autocoin.exchange.metadata

import automate.profit.autocoin.app.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import kotlinx.coroutines.runBlocking
import mu.KLogging

data class CommonExchangeCurrencyPairs(
    val currencyPairsToExchangePairs: Map<CurrencyPair, Set<ExchangePair>>,
    val exchangePairsToCurrencyPairs: Map<ExchangePair, Set<CurrencyPair>>,
    val exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange: Map<SupportedExchange, Set<CurrencyPair>>,
)

class CommonExchangeCurrencyPairsService(
    private val exchangeMetadataService: ExchangeMetadataService,
    private val currencyPairsWhiteList: Set<CurrencyPair> = emptySet(),
) {

    companion object : KLogging()

    lateinit var lastCalculatedCommonExchangeCurrencyPairs: CommonExchangeCurrencyPairs
        private set

    fun calculateCommonCurrencyPairs(): CommonExchangeCurrencyPairs {
        val currencyPairsToExchangePairs: MutableMap<CurrencyPair, MutableSet<ExchangePair>> = mutableMapOf()
        val exchangePairsToCurrencyPairs: MutableMap<ExchangePair, Set<CurrencyPair>> = mutableMapOf()
        val exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange: MutableMap<SupportedExchange, MutableSet<CurrencyPair>> =
            mutableMapOf()
        val exchangesWithCurrencyPairs = fetchExchangesWithCurrencyPairs()
        for (i in exchangesWithCurrencyPairs.indices) {
            for (j in i + 1 until exchangesWithCurrencyPairs.size) {
                val exchangePair =
                    ExchangePair(
                        SupportedExchange.fromExchangeName(exchangesWithCurrencyPairs[i].first),
                        SupportedExchange.fromExchangeName(exchangesWithCurrencyPairs[j].first)
                    )
                val commonCurrencyPairs =
                    findCommonCurrencyPairs(exchangesWithCurrencyPairs[i].second, exchangesWithCurrencyPairs[j].second)
                exchangePairsToCurrencyPairs[exchangePair] = commonCurrencyPairs
                commonCurrencyPairs.forEach {
                    currencyPairsToExchangePairs.putIfAbsent(it, mutableSetOf())
                    currencyPairsToExchangePairs[it]!!.add(exchangePair)

                    exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange.putIfAbsent(
                        exchangePair.firstExchange,
                        mutableSetOf()
                    )
                    exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange.putIfAbsent(
                        exchangePair.secondExchange,
                        mutableSetOf()
                    )
                    exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange[exchangePair.firstExchange]!!.add(it)
                    exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange[exchangePair.secondExchange]!!.add(it)
                }
            }
        }
        lastCalculatedCommonExchangeCurrencyPairs = CommonExchangeCurrencyPairs(
            currencyPairsToExchangePairs = currencyPairsToExchangePairs,
            exchangePairsToCurrencyPairs = exchangePairsToCurrencyPairs,
            exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange = exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange
        )
        return lastCalculatedCommonExchangeCurrencyPairs
    }

    private fun findCommonCurrencyPairs(
        firstExchangeCurrencyPairs: Set<CurrencyPair>,
        secondExchangeCurrencyPairs: Set<CurrencyPair>
    ): Set<CurrencyPair> {
        val currencyPairs = firstExchangeCurrencyPairs
            .filter { it in secondExchangeCurrencyPairs }.toSet()
        return if (currencyPairsWhiteList.isEmpty()) {
            currencyPairs
        } else {
            currencyPairs.filter { it in currencyPairsWhiteList }.toSet()
        }
    }

    private fun fetchExchangesWithCurrencyPairs(): Array<Pair<String, Set<CurrencyPair>>> {
        return runBlocking {
            val exchangesMetadata = exchangeMetadataService.getAllExchangesMetadata()
            exchangesMetadata.map { it.exchange.exchangeName to it.currencyPairMetadata.keys }.toTypedArray()
        }
    }
}
