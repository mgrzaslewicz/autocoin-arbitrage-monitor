package automate.profit.autocoin.metrics

import autocoin.metrics.MetricsService
import automate.profit.autocoin.exchange.arbitrage.orderbook.ExchangePairWithOpportunityCount
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageRelativeProfitGroup
import com.timgroup.statsd.StatsDClient

class MetricsService(private val statsDClient: StatsDClient) : MetricsService(statsDClient) {

    fun recordArbitrageOrderbooksSize(buyOrderbookSize: Long, sellOrderbookSize: Long, tags: String) {
        statsDClient.gauge("orderbook-buy-size,$tags", buyOrderbookSize)
        statsDClient.gauge("orderbook-sell-size,$tags", sellOrderbookSize)
    }

    fun recordArbitrageProfitCalculationTime(millis: Long, tags: String) {
        statsDClient.recordExecutionTime("profit,$tags", millis)
    }

    fun recordFetchPriceTime(millis: Long, tags: String) {
        statsDClient.recordExecutionTime("fetch-price-duration,$tags", millis)
    }

    fun recordExchangePairOpportunityCount(profitGroup: TwoLegArbitrageRelativeProfitGroup, exchangePairWithOpportunityCount: ExchangePairWithOpportunityCount) {
        val exchangePairTag =
            "${exchangePairWithOpportunityCount.exchangePair.firstExchange.exchangeName}-${exchangePairWithOpportunityCount.exchangePair.secondExchange.exchangeName}"
        statsDClient.gauge("exchange-pair-opportunity-count,exchangePair=$exchangePairTag,profitGroup=$profitGroup", exchangePairWithOpportunityCount.opportunityCount)
    }

}
