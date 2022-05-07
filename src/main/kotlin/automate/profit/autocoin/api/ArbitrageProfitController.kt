package automate.profit.autocoin.api

import automate.profit.autocoin.exchange.SupportedExchange
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


data class TwoLegOrderBookArbitrageOpportunityFeesDto(
    val buyFee: String?,
    val withdrawalFee: String?,
    val sellFee: String?,
)

data class TwoLegOrderBookArbitrageOpportunityDto(
    val sellPrice: String?,
    val sellAmount: String?,
    val sellAtExchange: SupportedExchange?,
    val buyPrice: String,
    val buyAmount: String,
    val buyAtExchange: SupportedExchange,
    val relativeProfitPercent: String,
    val profitUsd: String,
    val areDetailsHidden: Boolean,
    val usdDepthUpTo: String,
    val fees: TwoLegOrderBookArbitrageOpportunityFeesDto,
)

data class TwoLegArbitrageProfitDto(
    val baseCurrency: String,
    val counterCurrency: String,
    val firstExchange: SupportedExchange,
    val secondExchange: SupportedExchange?,
    val usd24hVolumeAtFirstExchange: String,
    val usd24hVolumeAtSecondExchange: String?,
    val arbitrageProfitHistogram: List<TwoLegOrderBookArbitrageOpportunityDto?>,
    val calculatedAtMillis: Long
)

data class TwoLegArbitrageResponseDto(
    val usdDepthThresholds: List<Int>,
    val profits: List<TwoLegArbitrageProfitDto?>
)

data class TwoLegArbitrageMetadataDto(
    val baseCurrenciesMonitored: Set<String>,
    val counterCurrenciesMonitored: Set<String>,
    val freePlanProfitPercentCutOff: String,
    val isIncludingProPlanOpportunities: Boolean,
)

private fun SecurityContext.authenticatedUserHasRole(roleName: String) = this.authenticatedAccount.roles.contains(roleName)

class ClientTwoLegArbitrageProfits(private val freePlanRelativeProfitCutOff: BigDecimal) {
    private val minRelativeProfit = 0.002.toBigDecimal()
    private val minUsd24hVolume = 1000.toBigDecimal()
    private val plusInfinity = Long.MAX_VALUE.toBigDecimal()
    private val maxRelativeProfitCutOff = BigDecimal("1.0")

    private fun TwoLegOrderBookArbitrageOpportunity.toDto(shouldHideOpportunityDetails: Boolean) = TwoLegOrderBookArbitrageOpportunityDto(
        sellPrice = if (shouldHideOpportunityDetails) null else sellPrice.setScale(8, HALF_EVEN).toPlainString(),
        sellAmount = if (shouldHideOpportunityDetails) null else baseCurrencyAmountAtSellExchange.setScale(8, HALF_EVEN).toPlainString(),
        buyPrice = buyPrice.setScale(8, HALF_EVEN).toPlainString(),
        buyAmount = baseCurrencyAmountAtBuyExchange.setScale(8, HALF_EVEN).toPlainString(),
        sellAtExchange = if (shouldHideOpportunityDetails) null else sellAtExchange,
        buyAtExchange = buyAtExchange,
        relativeProfitPercent = relativeProfit.movePointRight(2).setScale(4, HALF_EVEN).toPlainString(),
        profitUsd = profitUsd.setScale(2, HALF_EVEN).toPlainString(),
        areDetailsHidden = shouldHideOpportunityDetails,
        usdDepthUpTo = usdDepthUpTo.setScale(2, HALF_DOWN).toPlainString(),
        fees = TwoLegOrderBookArbitrageOpportunityFeesDto(
            buyFee = this.transactionFeeAmountBeforeTransfer?.setScale(8, HALF_EVEN)?.toPlainString(),
            withdrawalFee = this.transferFeeAmount?.setScale(8, HALF_EVEN)?.toPlainString(),
            sellFee = this.transactionFeeAmountAfterTransfer?.setScale(8, HALF_EVEN)?.toPlainString(),
        )
    )

    private fun TwoLegOrderBookArbitrageProfit.toDto(shouldHideOpportunityDetails: Boolean) = TwoLegArbitrageProfitDto(
        baseCurrency = currencyPairWithExchangePair.currencyPair.base,
        counterCurrency = currencyPairWithExchangePair.currencyPair.counter,
        firstExchange = currencyPairWithExchangePair.exchangePair.firstExchange,
        secondExchange = if (shouldHideOpportunityDetails) null else currencyPairWithExchangePair.exchangePair.secondExchange,
        usd24hVolumeAtFirstExchange = usd24hVolumeAtFirstExchange.setScale(2, HALF_DOWN).toPlainString(),
        usd24hVolumeAtSecondExchange = if (shouldHideOpportunityDetails) null else usd24hVolumeAtSecondExchange.setScale(2, HALF_DOWN).toPlainString(),
        arbitrageProfitHistogram = orderBookArbitrageProfitHistogram.map {
            it?.toDto(shouldHideOpportunityDetails)
        },
        calculatedAtMillis = calculatedAtMillis
    )

    fun process(allProfits: Sequence<TwoLegOrderBookArbitrageProfit>, isUserInProPlan: Boolean): List<TwoLegArbitrageProfitDto> {
        return allProfits.mapNotNull { profit ->
            val anyDepthHasProfitBetweenMinAndMax =
                profit.orderBookArbitrageProfitHistogram
                    .any { opportunity ->
                        (opportunity?.relativeProfit ?: ZERO) > minRelativeProfit
                                && (opportunity?.relativeProfit ?: plusInfinity) < maxRelativeProfitCutOff
                    }
            if (anyDepthHasProfitBetweenMinAndMax && profit.minUsd24hVolumeOfBothExchanges > minUsd24hVolume) {
                profit.toDto(
                    shouldHideOpportunityDetails = !isUserInProPlan
                            && profit.orderBookArbitrageProfitHistogram.any {
                        (it?.relativeProfit ?: ZERO) > freePlanRelativeProfitCutOff
                    }
                )
            } else null
        }
            .toList()
    }

}

class ArbitrageProfitController(
    private val twoLegOrderBookArbitrageProfitOpportunityCache: TwoLegOrderBookArbitrageProfitOpportunityCache,
    private val orderBookUsdAmountThresholds: List<BigDecimal>,
    private val commonExchangeCurrencyPairsService: CommonExchangeCurrencyPairsService,
    private val objectMapper: ObjectMapper,
    private val oauth2BearerTokenAuthHandlerWrapper: HttpHandlerWrapper,
    private val isUserInProPlanFunction: (httpServerExchange: HttpServerExchange) -> Boolean = { it -> it.securityContext.authenticatedUserHasRole("ROLE_PRO_USER") },
    private val clientTwoLegArbitrageProfits: ClientTwoLegArbitrageProfits,
    private val freePlanRelativeProfitPercentCutOff: String,
) : ApiController {

    private fun getTwoLegArbitrageProfits() = object : ApiHandler {
        override val method = GET
        override val urlTemplate = "/two-leg-arbitrage-profits"

        override val httpHandler = HttpHandler { httpServerExchange ->
            val isUserInProPlan = isUserInProPlanFunction(httpServerExchange)
            val profits = clientTwoLegArbitrageProfits.process(twoLegOrderBookArbitrageProfitOpportunityCache.getAllProfits(), isUserInProPlan)

            val result = TwoLegArbitrageResponseDto(
                usdDepthThresholds = orderBookUsdAmountThresholds.map { threshold -> threshold.toInt() },
                profits = profits
            )
            httpServerExchange.responseSender.send(objectMapper.writeValueAsString(result))
        }
            .authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)
    }

    private fun getTwoLegArbitrageMetadata() = object : ApiHandler {
        override val method = GET
        override val urlTemplate = "/two-leg-arbitrage-metadata"

        override val httpHandler = HttpHandler { httpServerExchange ->
            val lastCalculatedCommonExchangeCurrencyPairs = commonExchangeCurrencyPairsService.lastCalculatedCommonExchangeCurrencyPairs

            val baseCurrencies = lastCalculatedCommonExchangeCurrencyPairs.currencyPairsToExchangePairs.keys.map { it.base }.toSet()
            val counterCurrencies = lastCalculatedCommonExchangeCurrencyPairs.currencyPairsToExchangePairs.keys.map { it.counter }.toSet()

            val isUserInProPlan = isUserInProPlanFunction(httpServerExchange)

            val response = TwoLegArbitrageMetadataDto(
                baseCurrenciesMonitored = baseCurrencies,
                counterCurrenciesMonitored = counterCurrencies,
                freePlanProfitPercentCutOff = freePlanRelativeProfitPercentCutOff,
                isIncludingProPlanOpportunities = isUserInProPlan
            )
            httpServerExchange.responseSender.send(objectMapper.writeValueAsString(response))
        }
            .authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper)
    }

    override fun apiHandlers(): List<ApiHandler> = listOf(
        getTwoLegArbitrageProfits(),
        getTwoLegArbitrageMetadata()
    )

}
