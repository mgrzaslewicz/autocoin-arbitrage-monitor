package automate.profit.autocoin.api

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageOpportunity
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfit
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitCache
import automate.profit.autocoin.oauth.server.Oauth2BearerTokenAuthHandlerWrapper
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.util.Methods.GET
import java.math.BigDecimal.ZERO
import java.math.RoundingMode.HALF_DOWN

data class TwoLegOrderBookArbitrageOpportunityDto(
        val sellPrice: String,
        val buyPrice: String,
        val sellAtExchange: SupportedExchange,
        val buyAtExchange: SupportedExchange,
        val relativeProfitPercent: String,
        val usdDepthUpTo: String
)

data class TwoLegArbitrageProfitDto(
        val baseCurrency: String,
        val counterCurrency: String,
        val firstExchange: SupportedExchange,
        val secondExchange: SupportedExchange,
        val usd24hVolumeAtFirstExchange: String,
        val usd24hVolumeAtSecondExchange: String,
        val arbitrageProfitHistogram: List<TwoLegOrderBookArbitrageOpportunityDto?>,
        val calculatedAtMillis: Long
)

class ArbitrageProfitController(
        private val twoLegOrderBookArbitrageProfitCache: TwoLegOrderBookArbitrageProfitCache,
        private val objectMapper: ObjectMapper,
        private val oauth2BearerTokenAuthHandlerWrapper: Oauth2BearerTokenAuthHandlerWrapper
) : ApiController {

    private val minRelativeProfit = 0.003.toBigDecimal()
    private val minUsd24hVolume = 1000.toBigDecimal()

    private fun TwoLegOrderBookArbitrageOpportunity.toDto() = TwoLegOrderBookArbitrageOpportunityDto(
            sellPrice = sellPrice.setScale(8, HALF_DOWN).toPlainString(),
            buyPrice = buyPrice.setScale(8, HALF_DOWN).toPlainString(),
            sellAtExchange = sellAtExchange,
            buyAtExchange = buyAtExchange,
            relativeProfitPercent = relativeProfit.movePointRight(2).setScale(4, HALF_DOWN).toPlainString(),
            usdDepthUpTo = usdDepthUpTo.setScale(2, HALF_DOWN).toPlainString()
    )

    private fun TwoLegOrderBookArbitrageProfit.toDto() = TwoLegArbitrageProfitDto(
            baseCurrency = currencyPairWithExchangePair.currencyPair.base,
            counterCurrency = currencyPairWithExchangePair.currencyPair.counter,
            firstExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
            secondExchange = currencyPairWithExchangePair.exchangePair.secondExchange,
            usd24hVolumeAtFirstExchange = usd24hVolumeAtFirstExchange.setScale(2, HALF_DOWN).toPlainString(),
            usd24hVolumeAtSecondExchange = usd24hVolumeAtSecondExchange.setScale(2, HALF_DOWN).toPlainString(),
            arbitrageProfitHistogram = orderBookArbitrageProfitHistogram.map {
                it?.toDto()
            },
            calculatedAtMillis = calculatedAtMillis
    )

    private fun getTwoLegArbitrageProfits() = object : ApiHandler {
        override val method = GET
        override val urlTemplate = "/two-leg-arbitrage-profits"
        override val httpHandler = HttpHandler {
            val profits = twoLegOrderBookArbitrageProfitCache
                    .getCurrencyPairWithExchangePairs()
                    .mapNotNull { currencyPairWithExchangePair ->
                        val profit = twoLegOrderBookArbitrageProfitCache.getProfit(currencyPairWithExchangePair)
                        if (profit.orderBookArbitrageProfitHistogram
                                        .any { opportunity ->
                                            (opportunity?.relativeProfit ?: ZERO) > minRelativeProfit
                                        }
                                && profit.minUsd24hVolumeOfBothExchanges > minUsd24hVolume) {
                            profit.toDto()
                        } else null
                    }
            it.responseSender.send(objectMapper.writeValueAsString(profits))
        }.authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)
    }

    override fun apiHandlers(): List<ApiHandler> = listOf(
            getTwoLegArbitrageProfits()
    )

}