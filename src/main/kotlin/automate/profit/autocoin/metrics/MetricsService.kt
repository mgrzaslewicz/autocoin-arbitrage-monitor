package automate.profit.autocoin.metrics

import autocoin.metrics.MetricsService
import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.ExchangePairWithOpportunityCount
import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.timgroup.statsd.StatsDClient

class MetricsService(private val statsDClient: StatsDClient) : MetricsService(statsDClient) {

    fun recordArbitrageOrderbooksSize(buyOrderbookSize: Long, sellOrderbookSize: Long, tags: String) {
        statsDClient.gauge("orderbook-buy-size,$tags", buyOrderbookSize)
        statsDClient.gauge("orderbook-sell-size,$tags", sellOrderbookSize)
    }

    fun recordFetchPriceTime(millis: Long, tags: String) {
        statsDClient.recordExecutionTime("fetch-price-duration,$tags", millis)
    }

    fun recordExchangePairOpportunityCount(exchangePairWithOpportunityCount: ExchangePairWithOpportunityCount) {
        val exchangePairTag =
            "${exchangePairWithOpportunityCount.exchangePair.firstExchange.exchangeName}-${exchangePairWithOpportunityCount.exchangePair.secondExchange.exchangeName}"
        statsDClient.gauge("exchange-pair-opportunity-count,exchangePair=$exchangePairTag", exchangePairWithOpportunityCount.opportunityCount)
    }

    fun recordExchangeOpportunityCount(exchange: SupportedExchange, opportunityCount: Long) {
        statsDClient.gauge("exchange-opportunity-count,exchange=${exchange.exchangeName}", opportunityCount)
    }

    /**
     * When exchange has value 0 in statistics it means it was not calculated at all
     */
    fun recordExchangeNoOpportunityFoundCount(exchange: SupportedExchange, noOpportunityFoundCount: Long) {
        statsDClient.gauge("exchange-no-opportunity-found-count,exchange=${exchange.exchangeName}", noOpportunityFoundCount)
    }

    fun recordNoOrderBookForTwoLegProfitOpportunityCalculation(
        exchange: SupportedExchange, currencyPair: CurrencyPair
    ) {
        recordNoDataForTwoLegProfitOpportunityCalculation(exchange, "missing=order-book,currencyPair=$currencyPair,reason=not-received")
    }

    fun recordNoTickerForTwoLegProfitOpportunityCalculation(exchange: SupportedExchange, currencyPair: CurrencyPair) {
        recordNoDataForTwoLegProfitOpportunityCalculation(exchange, "exchange=${exchange.exchangeName},missing=ticker,currencyPair=$currencyPair,reason=not-received")
    }

    fun recordNoUsdPriceForTwoLegProfitOpportunityCalculation(exchangePair: ExchangePair, currencyPair: CurrencyPair, reasonTag: String) {
        recordNoDataForTwoLegProfitOpportunityCalculation(exchangePair.firstExchange, "missing=usd-price,currencyPair=$currencyPair,reason=$reasonTag")
        recordNoDataForTwoLegProfitOpportunityCalculation(exchangePair.secondExchange, "missing=usd-price,currencyPair=$currencyPair,reason=$reasonTag")
    }

    private fun recordNoDataForTwoLegProfitOpportunityCalculation(exchange: SupportedExchange, tags: String) {
        statsDClient.gauge("missing-data-for-opportunity-calculation,exchange=${exchange.exchangeName},$tags", 1)
    }
}
