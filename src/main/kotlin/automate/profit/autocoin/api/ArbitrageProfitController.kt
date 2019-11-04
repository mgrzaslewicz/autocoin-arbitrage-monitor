package automate.profit.autocoin.api

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfit
import automate.profit.autocoin.exchange.arbitrage.TwoLegArbitrageProfitCache
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.server.HttpHandler
import io.undertow.util.Methods.GET
import java.math.RoundingMode.HALF_DOWN

data class TwoLegArbitrageProfitDto(
        val baseCurrency: String,
        val counterCurrency: String,
        val sellAtExchange: SupportedExchange,
        val buyAtExchange: SupportedExchange,
        val sellPrice: Double,
        val buyPrice: Double,
        val relativeProfitPercent: Double
)

class ArbitrageProfitController(
        private val twoLegArbitrageProfitCache: TwoLegArbitrageProfitCache,
        private val objectMapper: ObjectMapper
) : ApiController {

    private fun TwoLegArbitrageProfit.toDto() = TwoLegArbitrageProfitDto(
            baseCurrency = currencyPair.base,
            counterCurrency = currencyPair.counter,
            sellAtExchange = sellAtExchange,
            buyAtExchange = buyAtExchange,
            sellPrice = sellPrice.setScale(8).toDouble(),
            buyPrice = buyPrice.setScale(8).toDouble(),
            relativeProfitPercent = relativeProfit.movePointRight(2).setScale(4, HALF_DOWN).toDouble()
    )

    private fun getTwoLegArbitrageProfits() = object : ApiHandler {
        override fun method() = GET
        override fun urlTemplate() = "/two-leg-arbitrage-profits"
        override fun httpHandler() = HttpHandler {
            val minimumRelativeProfit = 0.005.toBigDecimal()
            val profits = twoLegArbitrageProfitCache
                    .getCurrencyPairWithExchangePairs()
                    .flatMap { currencyPairWithExchangePair ->
                        twoLegArbitrageProfitCache.getProfits(currencyPairWithExchangePair)
                                .filter{twoLegArbitrageProfit ->
                                    twoLegArbitrageProfit.relativeProfit > minimumRelativeProfit
                                }
                                .map { twoLegArbitrageProfit ->
                                   twoLegArbitrageProfit.toDto()
                                }
                    }
            it.responseSender.send(objectMapper.writeValueAsString(profits))
        }
    }

    override fun apiHandlers(): List<ApiHandler> = listOf(
            getTwoLegArbitrageProfits()
    )

}