package automate.profit.autocoin.api

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.statistic.ProfitOpportunityCount
import automate.profit.autocoin.exchange.arbitrage.statistic.ProfitStatisticHistogramByUsdDepth
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatistic
import automate.profit.autocoin.exchange.arbitrage.statistic.TwoLegArbitrageProfitStatisticsCache
import automate.profit.autocoin.metrics.Oauth2MetricsHandlerWrapper
import automate.profit.autocoin.metrics.countEndpointUsage
import automate.profit.autocoin.oauth.server.Oauth2BearerTokenAuthHandlerWrapper
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.util.Methods.GET
import java.math.BigDecimal
import java.math.RoundingMode.HALF_DOWN

data class ProfitOpportunityCountDto(
        val profitPercentThreshold: String,
        val count: Int
)

data class ProfitStatisticHistogramByUsdDepthDto(
        val usdDepthTo: String,
        val profitOpportunityHistogram: List<ProfitOpportunityCountDto>,
        val minProfitPercent: String,
        val maxProfitPercent: String,
        val averageProfitPercent: String
)

data class TwoLegArbitrageProfitStatisticDto(
        val baseCurrency: String,
        val counterCurrency: String,
        val firstExchange: SupportedExchange,
        val secondExchange: SupportedExchange,
        val minUsd24hVolume: String,
        val profitStatisticHistogramByUsdDepth: List<ProfitStatisticHistogramByUsdDepthDto>
)

class ArbitrageProfitStatisticsController(
        private val twoLegArbitrageProfitStatisticsCache: TwoLegArbitrageProfitStatisticsCache,
        private val objectMapper: ObjectMapper,
        private val oauth2BearerTokenAuthHandlerWrapper: Oauth2BearerTokenAuthHandlerWrapper,
        private val oauth2MetricsHandlerWrapper: Oauth2MetricsHandlerWrapper
) : ApiController {

    val minRelativeProfit = 0.003.toBigDecimal()
    val minUsd24hVolume = 1000.toBigDecimal()

    private fun BigDecimal.toPercentWith2DecimalPlaces(): String {
        return try {
            movePointRight(2).setScale(2).toPlainString()
        } catch (e: ArithmeticException) {
            movePointRight(2).setScale(2, HALF_DOWN).toPlainString()
        }
    }

    private fun BigDecimal.with2DecimalPlaces(): String {
        return try {
            setScale(2).toPlainString()
        } catch (e: ArithmeticException) {
            setScale(2, HALF_DOWN).toPlainString()
        }
    }

    private fun ProfitOpportunityCount.toDto() = ProfitOpportunityCountDto(
            profitPercentThreshold = relativeProfitThreshold.toPercentWith2DecimalPlaces(),
            count = count
    )

    private fun ProfitStatisticHistogramByUsdDepth.toDto() = ProfitStatisticHistogramByUsdDepthDto(
            usdDepthTo = usdDepthTo.with2DecimalPlaces(),
            profitOpportunityHistogram = profitOpportunityHistogram.map { it.toDto() },
            averageProfitPercent = average.toPercentWith2DecimalPlaces(),
            minProfitPercent = min.toPercentWith2DecimalPlaces(),
            maxProfitPercent = max.toPercentWith2DecimalPlaces()
    )

    private fun TwoLegArbitrageProfitStatistic.toDto() = TwoLegArbitrageProfitStatisticDto(
            baseCurrency = currencyPairWithExchangePair.currencyPair.base,
            counterCurrency = currencyPairWithExchangePair.currencyPair.counter,
            firstExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            secondExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
            minUsd24hVolume = minUsd24hVolume.with2DecimalPlaces(),
            profitStatisticHistogramByUsdDepth = profitStatisticHistogramByUsdDepth.map { it.toDto() }
    )

    private fun getTwoLegArbitrageProfitStatistics() = object : ApiHandler {
        override val method = GET
        override val urlTemplate = "/two-leg-arbitrage-profit-statistics"
        override val httpHandler = HttpHandler { serverExchange ->
            val profits = twoLegArbitrageProfitStatisticsCache.twoLegArbitrageProfitStatistics.get()
                    .filter {
                        it.profitStatisticHistogramByUsdDepth
                                .any { stat -> stat.max > minRelativeProfit } && it.minUsd24hVolume > minUsd24hVolume
                    }
                    .map { it.toDto() }
            serverExchange.responseSender.send(objectMapper.writeValueAsString(profits))
        }
                .countEndpointUsage(oauth2MetricsHandlerWrapper)
                .authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)
    }

    override fun apiHandlers(): List<ApiHandler> = listOf(
            getTwoLegArbitrageProfitStatistics()
    )

}