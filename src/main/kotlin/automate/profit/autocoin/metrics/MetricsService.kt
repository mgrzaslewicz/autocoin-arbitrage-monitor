package automate.profit.autocoin.metrics

import autocoin.metrics.MetricsService
import com.timgroup.statsd.StatsDClient

class MetricsService(private val statsDClient: StatsDClient) : MetricsService(statsDClient) {

    fun recordArbitrageOrderbooksSize(buyOrderbookSize: Long, sellOrderbookSize: Long, commonTags: String) {
        statsDClient.gauge("orderbook-buy-size,$commonTags", buyOrderbookSize)
        statsDClient.gauge("orderbook-sell-size,$commonTags", sellOrderbookSize)
    }

    fun recordArbitrageProfitCalculationTime(millis: Long, commonTags: String) {
        statsDClient.recordExecutionTime("profit,$commonTags", millis)
    }

}