package automate.profit.autocoin.metrics

import autocoin.metrics.MetricsService
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

}