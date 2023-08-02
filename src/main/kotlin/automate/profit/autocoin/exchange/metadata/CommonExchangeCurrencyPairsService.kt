package automate.profit.autocoin.exchange.metadata

import automate.profit.autocoin.app.config.ExchangePair
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.ExchangeProvider
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.metadata.gateway.AuthorizedMetadataServiceGateway
import kotlinx.coroutines.runBlocking
import mu.KLogging

data class CommonExchangeCurrencyPairs(
    val currencyPairsToExchangePairs: Map<CurrencyPair, Set<ExchangePair>>,
    val exchangePairsToCurrencyPairs: Map<ExchangePair, Set<CurrencyPair>>,
    val exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange: Map<Exchange, Set<CurrencyPair>>,
)

class CommonExchangeCurrencyPairsService(
    private val exchangeMetadataServiceGateway: AuthorizedMetadataServiceGateway,
    private val currencyPairsWhiteList: Set<CurrencyPair> = emptySet(),
    private val exchangeProvider: ExchangeProvider,
) {

    companion object : KLogging()

    lateinit var lastCalculatedCommonExchangeCurrencyPairs: CommonExchangeCurrencyPairs
        private set

    fun calculateCommonCurrencyPairs(): CommonExchangeCurrencyPairs {
        val currencyPairsToExchangePairs: MutableMap<CurrencyPair, MutableSet<ExchangePair>> = mutableMapOf()
        val exchangePairsToCurrencyPairs: MutableMap<ExchangePair, Set<CurrencyPair>> = mutableMapOf()
        val exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange: MutableMap<Exchange, MutableSet<CurrencyPair>> =
            mutableMapOf()
        val exchangesWithCurrencyPairs = fetchExchangesWithCurrencyPairs()
        for (i in exchangesWithCurrencyPairs.indices) {
            for (j in i + 1 until exchangesWithCurrencyPairs.size) {
                val exchangePair =
                    ExchangePair(
                        exchangeProvider.getExchange(exchangesWithCurrencyPairs[i].first),
                        exchangeProvider.getExchange(exchangesWithCurrencyPairs[j].first),
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
            val exchangesMetadata = exchangeMetadataServiceGateway.getAllExchangesMetadata()
            exchangesMetadata.map { it.key.exchangeName to it.value.currencyPairMetadata.keys }.toTypedArray()
        }
    }
}
