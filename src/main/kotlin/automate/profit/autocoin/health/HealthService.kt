package automate.profit.autocoin.health

import automate.profit.autocoin.exchange.ExchangeWithCurrencyPair
import automate.profit.autocoin.exchange.arbitrage.orderbook.ExchangePairWithOpportunityCount
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCache
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.exchange.orderbook.OrderBookListeners
import automate.profit.autocoin.exchange.ticker.TickerListeners
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.spi.exchange.metadata.gateway.AuthorizedMetadataServiceGateway
import com.autocoin.exchangegateway.spi.exchange.orderbook.OrderBook
import com.autocoin.exchangegateway.spi.exchange.orderbook.listener.OrderBookListener
import com.autocoin.exchangegateway.spi.exchange.ticker.Ticker
import com.autocoin.exchangegateway.spi.exchange.ticker.listener.TickerListener
import java.util.*

data class TwoLegArbitrageOpportunitiesCount(
    val currentTotalCount: Long,
    val exchangePairsWithOpportunityCount: List<ExchangePairWithOpportunityCount>,
)

data class Health(
    val version: String?,
    val healthy: Boolean,
    val healthChecks: List<HealthCheckResult>,
    val twoLegArbitrageOpportunities: TwoLegArbitrageOpportunitiesCount,
    val receivedOrderBooksSinceStart: Map<Exchange, Long>,
    val receivedTickersSinceStart: Map<Exchange, Long>,
    /**
     * e.g. "bittrex-binance" to 100
     */
    val commonExchangePairsToCurrencyPairs: Map<String, Int>,
    /**
     * e.g. "bittrex" to setOf("XRP/USDT", "ETH/BTC")
     */
    val exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange: Map<Exchange, Set<String>>,
)


class HealthService(
    private val healthChecks: List<HealthCheck>,
    private val appVersion: String?,
    private val metadataServiceGateway: AuthorizedMetadataServiceGateway,
    private val commonExchangeCurrencyPairsService: CommonExchangeCurrencyPairsService,
    private val twoLegArbitrageProfitOpportunityCache: TwoLegArbitrageProfitOpportunityCache,
) {
    private val orderBookUpdatesSinceStart: MutableMap<Exchange, Long> by lazy {
        val count = HashMap<Exchange, Long>()
        metadataServiceGateway.getAllExchangesMetadata().forEach {
            count[it.key] = 0L
        }
        count
    }

    private val tickerUpdatesSinceStart: MutableMap<Exchange, Long> by lazy {
        val count = HashMap<Exchange, Long>()
        metadataServiceGateway.getAllExchangesMetadata().forEach {
            count[it.key] = 0L
        }
        count
    }

    fun addOrderBookListenersTo(orderBookListeners: OrderBookListeners) {
        val orderBookListener = object : OrderBookListener {
            override fun onOrderBook(exchange: Exchange, currencyPair: CurrencyPair, orderBook: OrderBook) {
                this@HealthService.onOrderBook(exchange)
            }
        }
        commonExchangeCurrencyPairsService.lastCalculatedCommonExchangeCurrencyPairs.currencyPairsToExchangePairs.forEach {
            it.value.map { exchangePair ->
                orderBookListeners.addOrderBookListener(
                    ExchangeWithCurrencyPair(
                        exchange = exchangePair.firstExchange,
                        currencyPair = it.key
                    ), orderBookListener
                )
                orderBookListeners.addOrderBookListener(
                    ExchangeWithCurrencyPair(
                        exchange = exchangePair.secondExchange,
                        currencyPair = it.key
                    ), orderBookListener
                )
            }
        }
    }

    fun addTickerListenersTo(tickerListeners: TickerListeners) {
        val orderBookListener = object : TickerListener {
            override fun onTicker(exchange: Exchange, currencyPair: CurrencyPair, ticker: Ticker) {
                this@HealthService.onTicker(exchange)
            }
        }
        commonExchangeCurrencyPairsService.lastCalculatedCommonExchangeCurrencyPairs.currencyPairsToExchangePairs.forEach {
            it.value.map { exchangePair ->
                tickerListeners.addTickerListener(
                    ExchangeWithCurrencyPair(
                        exchange = exchangePair.firstExchange,
                        currencyPair = it.key
                    ), orderBookListener
                )
                tickerListeners.addTickerListener(
                    ExchangeWithCurrencyPair(
                        exchange = exchangePair.secondExchange,
                        currencyPair = it.key
                    ), orderBookListener
                )
            }
        }
    }

    private fun onOrderBook(exchange: Exchange) {
        orderBookUpdatesSinceStart[exchange] = orderBookUpdatesSinceStart[exchange]!! + 1
    }


    private fun onTicker(exchange: Exchange) {
        tickerUpdatesSinceStart[exchange] = orderBookUpdatesSinceStart[exchange]!! + 1
    }

    fun getHealth(): Health {
        val healthCheckResults = healthChecks.map { it() }
        val twoLegArbitrageExchangePairsOpportunityCount =
            twoLegArbitrageProfitOpportunityCache.getExchangePairsOpportunityCount()
        val lastCalculatedCommonExchangeCurrencyPairs =
            commonExchangeCurrencyPairsService.lastCalculatedCommonExchangeCurrencyPairs
        val health = Health(
            healthy = healthCheckResults.all { it.healthy },
            healthChecks = healthCheckResults,
            commonExchangePairsToCurrencyPairs = lastCalculatedCommonExchangeCurrencyPairs.exchangePairsToCurrencyPairs.map {
                "${it.key.firstExchange}/${it.key.secondExchange}" to it.value.size
            }
                .sortedBy { it.second }
                .toMap(),
            exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange = lastCalculatedCommonExchangeCurrencyPairs.exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange.map { exchangeToCurrencyPairs ->
                exchangeToCurrencyPairs.key to exchangeToCurrencyPairs.value.map { it.toString() }.toSortedSet()
            }
                .sortedBy { it.first.exchangeName }
                .toMap(),
            receivedOrderBooksSinceStart = Collections.unmodifiableMap(
                orderBookUpdatesSinceStart.toList().sortedBy { it.second }.toMap()
            ),
            receivedTickersSinceStart = Collections.unmodifiableMap(
                tickerUpdatesSinceStart.toList().sortedBy { it.second }.toMap()
            ),
            twoLegArbitrageOpportunities = TwoLegArbitrageOpportunitiesCount(
                exchangePairsWithOpportunityCount = twoLegArbitrageExchangePairsOpportunityCount,
                currentTotalCount = twoLegArbitrageExchangePairsOpportunityCount.sumOf { it.opportunityCount },
            ),
            version = appVersion,
        )
        return health
    }
}
