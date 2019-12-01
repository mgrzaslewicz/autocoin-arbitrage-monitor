package automate.profit.autocoin.api

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfit
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitCache
import automate.profit.autocoin.oauth.server.Oauth2BearerTokenAuthHandlerWrapper
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.util.Methods.GET
import java.math.RoundingMode.HALF_DOWN

data class TwoLegArbitrageProfitDto(
        val baseCurrency: String,
        val counterCurrency: String,
        val sellAtExchange: SupportedExchange,
        val usd24hVolumeAtSellExchange: String,
        val buyAtExchange: SupportedExchange,
        val usd24hVolumeAtBuyExchange: String,
        val sellPrice: String,
        val buyPrice: String,
        val relativeProfitPercent: String,
        val calculatedAtMillis: Long
)

class ArbitrageProfitController(
        private val twoLegArbitrageProfitCache: TwoLegArbitrageProfitCache,
        private val objectMapper: ObjectMapper,
        private val oauth2BearerTokenAuthHandlerWrapper: Oauth2BearerTokenAuthHandlerWrapper
) : ApiController {

    val minRelativeProfit = 0.003.toBigDecimal()
    val minUsd24hVolume = 1000.toBigDecimal()

    private fun TwoLegArbitrageProfit.toDto() = TwoLegArbitrageProfitDto(
            baseCurrency = currencyPair.base,
            counterCurrency = currencyPair.counter,
            sellAtExchange = sellAtExchange,
            usd24hVolumeAtSellExchange = usd24hVolumeAtSellExchange.setScale(2, HALF_DOWN).toPlainString(),
            buyAtExchange = buyAtExchange,
            usd24hVolumeAtBuyExchange = usd24hVolumeAtBuyExchange.setScale(2, HALF_DOWN).toPlainString(),
            sellPrice = sellPrice.setScale(8, HALF_DOWN).toPlainString(),
            buyPrice = buyPrice.setScale(8, HALF_DOWN).toPlainString(),
            relativeProfitPercent = relativeProfit.movePointRight(2).setScale(4, HALF_DOWN).toPlainString(),
            calculatedAtMillis = calculatedAtMillis
    )

    private fun getTwoLegArbitrageProfits() = object : ApiHandler {
        override val method = GET
        override val urlTemplate = "/two-leg-arbitrage-profits"
        override val httpHandler = HttpHandler {
            val profits = twoLegArbitrageProfitCache
                    .getCurrencyPairWithExchangePairs()
                    .mapNotNull { currencyPairWithExchangePair ->
                        val profit = twoLegArbitrageProfitCache.getProfit(currencyPairWithExchangePair)
                        if (profit.relativeProfit > minRelativeProfit && profit.minUsd24hVolumeOfBothExchanges > minUsd24hVolume) {
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