package automate.profit.autocoin.metrics

import autocoin.metrics.MetricsService
import automate.profit.autocoin.app.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.ExchangePairWithOpportunityCount
import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.timgroup.statsd.StatsDClient

class MetricsService(private val statsDClient: StatsDClient) : MetricsService(statsDClient) {

    fun recordFetchPriceTime(millis: Long, tags: String) {
        statsDClient.recordExecutionTime("fetch-price-duration,$tags", millis)
    }

    fun recordExchangePairOpportunityCount(exchangePairWithOpportunityCount: ExchangePairWithOpportunityCount) {
        val exchangePairTag =
            "${exchangePairWithOpportunityCount.exchangePair.firstExchange.exchangeName}-${exchangePairWithOpportunityCount.exchangePair.secondExchange.exchangeName}"
        statsDClient.gauge("exchange-pair-opportunity-count,exchangePair=$exchangePairTag", exchangePairWithOpportunityCount.opportunityCount)
    }

    fun recordReceivedOrderBooksSinceStart(exchangeName: String, total: Long) {
        statsDClient.gauge("exchange-received-order-books-count,exchange=$exchangeName", total)
    }
    fun recordReceivedTickersSinceStart(exchangeName: String, count: Long) {
        statsDClient.gauge("exchange-received-tickers-count,exchange=$exchangeName", count)
    }

    fun recordNoUsdPriceForTwoLegProfitOpportunityCalculation(exchangePair: ExchangePair, currencyPair: CurrencyPair, reasonTag: String) {
        recordNoDataForTwoLegProfitOpportunityCalculation(exchangePair.firstExchange, "missing=usd-price,currencyPair=$currencyPair,reason=$reasonTag")
        recordNoDataForTwoLegProfitOpportunityCalculation(exchangePair.secondExchange, "missing=usd-price,currencyPair=$currencyPair,reason=$reasonTag")
    }

    private fun recordNoDataForTwoLegProfitOpportunityCalculation(exchange: SupportedExchange, tags: String) {
        statsDClient.gauge("missing-data-for-opportunity-calculation,exchange=${exchange.exchangeName},$tags", 1)
    }
}
