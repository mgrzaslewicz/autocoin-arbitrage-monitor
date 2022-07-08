package automate.profit.autocoin.api

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageRelativeProfitGroup
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageOpportunity
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfit
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitOpportunityCache
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.oauth.server.authorizeWithOauth2
import com.fasterxml.jackson.databind.ObjectMapper
import io.undertow.security.api.SecurityContext
import io.undertow.server.HttpHandler
import io.undertow.server.HttpServerExchange
import io.undertow.util.Methods.GET
import java.math.BigDecimal
import java.math.BigDecimal.ZERO
import java.math.RoundingMode.HALF_DOWN
import java.math.RoundingMode.HALF_EVEN

data class TwoLegOrderBookArbitrageOpportunityDto(
    val sellPrice: String,
    val sellAmount: String,
    val sellAtExchange: SupportedExchange,
    val buyPrice: String,
    val buyAmount: String,
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

data class TwoLegArbitrageResponseDto(
    val usdDepthThresholds: List<Int>,
    val profits: List<TwoLegArbitrageProfitDto?>
)

data class TwoLegArbitrageMetadataDto(
    val baseCurrenciesMonitored: Set<String>,
    val counterCurrenciesMonitored: Set<String>
)

class ArbitrageProfitController(
    private val twoLegOrderBookArbitrageProfitOpportunityCache: TwoLegOrderBookArbitrageProfitOpportunityCache,
    private val orderBookUsdAmountThresholds: List<BigDecimal>,
    private val commonExchangeCurrencyPairsService: CommonExchangeCurrencyPairsService,
    private val objectMapper: ObjectMapper,
    private val oauth2BearerTokenAuthHandlerWrapper: HttpHandlerWrapper
) : ApiController {

    private val minRelativeProfit = 0.002.toBigDecimal()
    private val minUsd24hVolume = 1000.toBigDecimal()

    private fun TwoLegOrderBookArbitrageOpportunity.toDto() = TwoLegOrderBookArbitrageOpportunityDto(
        sellPrice = sellPrice.setScale(8, HALF_EVEN).toPlainString(),
        sellAmount = baseCurrencyAmountAtSellExchange.setScale(8, HALF_EVEN).toPlainString(),
        buyPrice = buyPrice.setScale(8, HALF_EVEN).toPlainString(),
        buyAmount = baseCurrencyAmountAtBuyExchange.setScale(8, HALF_EVEN).toPlainString(),
        sellAtExchange = sellAtExchange,
        buyAtExchange = buyAtExchange,
        relativeProfitPercent = relativeProfit.movePointRight(2).setScale(4, HALF_EVEN).toPlainString(),
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

    private fun SecurityContext.authenticatedUserHasRole(roleName: String) = this.authenticatedAccount.roles.contains(roleName)

    private fun getProfitGroupForUser(httpServerExchange: HttpServerExchange): TwoLegArbitrageRelativeProfitGroup {
        return when {
            httpServerExchange.securityContext.authenticatedUserHasRole("ROLE_DETAILED_ARBITRAGE_USER") ->
                TwoLegArbitrageRelativeProfitGroup.ACCURATE_USING_METADATA
            else -> TwoLegArbitrageRelativeProfitGroup.INACCURATE_NOT_USING_METADATA
        }
    }

    private fun getTwoLegArbitrageProfits() = object : ApiHandler {
        override val method = GET
        override val urlTemplate = "/two-leg-arbitrage-profits"

        override val httpHandler = HttpHandler { httpServerExchange ->
            val profitGroup = getProfitGroupForUser(httpServerExchange)
            val profits = twoLegOrderBookArbitrageProfitOpportunityCache
                .getCurrencyPairWithExchangePairs(profitGroup)
                .asSequence()
                .mapNotNull { currencyPairWithExchangePair ->
                    val profit = twoLegOrderBookArbitrageProfitOpportunityCache
                        .getProfit(profitGroup, currencyPairWithExchangePair)
                    if (profit?.orderBookArbitrageProfitHistogram
                            ?.any { opportunity ->
                                (opportunity?.relativeProfit ?: ZERO) > minRelativeProfit
                            } == true
                        && profit.minUsd24hVolumeOfBothExchanges > minUsd24hVolume
                    ) {
                        profit.toDto()
                    } else null
                }
                .toList()
            val result = TwoLegArbitrageResponseDto(
                usdDepthThresholds = orderBookUsdAmountThresholds.map { threshold -> threshold.toInt() },
                profits = profits
            )
            httpServerExchange.responseSender.send(objectMapper.writeValueAsString(result))
        }
            .authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)
    }

    private fun getTwoLegAribtrageMetadata() = object : ApiHandler {
        override val method = GET
        override val urlTemplate = "/two-leg-arbitrage-metadata"

        override val httpHandler = HttpHandler { httpServerExchange ->
            val lastCalculatedCommonExchangeCurrencyPairs = commonExchangeCurrencyPairsService.lastCalculatedCommonExchangeCurrencyPairs

            val baseCurrencies = lastCalculatedCommonExchangeCurrencyPairs.currencyPairsToExchangePairs.keys.map { it.base }.toSet()
            val counterCurrencies = lastCalculatedCommonExchangeCurrencyPairs.currencyPairsToExchangePairs.keys.map { it.counter }.toSet()

            val response = TwoLegArbitrageMetadataDto(
                baseCurrenciesMonitored = baseCurrencies,
                counterCurrenciesMonitored = counterCurrencies
            )
            httpServerExchange.responseSender.send(objectMapper.writeValueAsString(response))
        }
            .authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)
    }

    override fun apiHandlers(): List<ApiHandler> = listOf(
        getTwoLegArbitrageProfits(),
        getTwoLegAribtrageMetadata()
    )

}
