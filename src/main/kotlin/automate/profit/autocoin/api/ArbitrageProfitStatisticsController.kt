package automate.profit.autocoin.api

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.statistic.ProfitOpportunityCount
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatistic
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCache
import automate.profit.autocoin.oauth.server.Oauth2BearerTokenAuthHandlerWrapper
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.util.Methods.GET
import java.math.BigDecimal
import java.math.RoundingMode.HALF_DOWN

data class ProfitOpportunityCountDto(
        val profitPercentThreshold: Double,
        val count: Int
)

data class TwoLegArbitrageProfitStatisticDto(
        val baseCurrency: String,
        val counterCurrency: String,
        val firstExchange: SupportedExchange,
        val secondExchange: SupportedExchange,
        val minProfitPercent: Double,
        val maxProfitPercent: Double,
        val averageProfitPercent: Double,
        val profitOpportunityHistogram: List<ProfitOpportunityCountDto>
)

class ArbitrageProfitStatisticsController(
        private val twoLegArbitrageProfitStatisticsCache: TwoLegArbitrageProfitStatisticsCache,
        private val objectMapper: ObjectMapper,
        private val oauth2BearerTokenAuthHandlerWrapper: Oauth2BearerTokenAuthHandlerWrapper
) : ApiController {

    val minimumRelativeProfit = 0.005.toBigDecimal()

    private fun BigDecimal.toPercent(): Double {
        return try {
            this.movePointRight(2).setScale(2).toDouble()
        } catch (e: ArithmeticException) {
            this.movePointRight(2).setScale(2, HALF_DOWN).toDouble()
        }
    }

    private fun ProfitOpportunityCount.toDto() = ProfitOpportunityCountDto(
            profitPercentThreshold = relativeProfitThreshold.toPercent(),
            count = count
    )

    private fun TwoLegArbitrageProfitStatistic.toDto() = TwoLegArbitrageProfitStatisticDto(
            baseCurrency = currencyPairWithExchangePair.currencyPair.base,
            counterCurrency = currencyPairWithExchangePair.currencyPair.counter,
            firstExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            secondExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
            minProfitPercent = min.toPercent(),
            maxProfitPercent = max.toPercent(),
            averageProfitPercent = average.toPercent(),
            profitOpportunityHistogram = profitOpportunityHistogram.map { it.toDto() }
    )

    private fun getTwoLegArbitrageProfitStatistics() = object : ApiHandler {
        override val method = GET
        override val urlTemplate = "/two-leg-arbitrage-profit-statistics"
        override val httpHandler = HttpHandler { serverExchange ->
            val profits = twoLegArbitrageProfitStatisticsCache.twoLegArbitrageProfitStatistics.get()
                    .filter { it.max > minimumRelativeProfit }
                    .map { it.toDto() }
            serverExchange.responseSender.send(objectMapper.writeValueAsString(profits))
        }.authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)
    }

    override fun apiHandlers(): List<ApiHandler> = listOf(
            getTwoLegArbitrageProfitStatistics()
    )

}