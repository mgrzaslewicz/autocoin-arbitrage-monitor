package automate.profit.autocoin.api

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunity
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityAtDepth
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCache
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


data class TwoLegArbitrageProfitOpportunityFeesDto(
    val buyFee: String?,
    val isDefaultBuyFeeUsed: Boolean,
    val withdrawalFee: String?,
    val sellFee: String?,
    val isDefaultSellFeeUsed: Boolean,
)

data class TwoLegArbitrageProfitOpportunityAtDepthDto(
    val sellPrice: String?,
    val sellAmount: String?,
    val buyPrice: String,
    val buyAmount: String,
    val relativeProfitPercent: String,
    val profitUsd: String,
    val usdDepthUpTo: String,
    val fees: TwoLegArbitrageProfitOpportunityFeesDto,
)

data class TwoLegArbitrageProfitOpportunityDto(
    val baseCurrency: String,
    val counterCurrency: String,
    val buyAtExchange: SupportedExchange,
    val sellAtExchange: SupportedExchange?,
    val usd24hVolumeAtBuyExchange: String,
    val usd24hVolumeAtSellExchange: String?,
    val profitOpportunityHistogram: List<TwoLegArbitrageProfitOpportunityAtDepthDto?>,
    val areDetailsHidden: Boolean,
    val calculatedAtMillis: Long
)

data class TwoLegArbitrageProfitOpportunitiesResponseDto(
    val usdDepthThresholds: List<Int>,
    val profits: List<TwoLegArbitrageProfitOpportunityDto?>
)

data class TwoLegArbitrageMetadataDto(
    val baseCurrenciesMonitored: Set<String>,
    val counterCurrenciesMonitored: Set<String>,
    val freePlanProfitPercentCutOff: String,
    val isIncludingProPlanOpportunities: Boolean,
    val defaultTransactionFeePercent: String,
)

private fun SecurityContext.authenticatedUserHasRole(roleName: String) = this.authenticatedAccount.roles.contains(roleName)

class ClientTwoLegArbitrageProfitOpportunities(
    private val freePlanRelativeProfitCutOff: BigDecimal,
    /**
     * It does not make sense to expose too small opportunities (like small fraction of a percent) as no one will use it
     */
    private val minRelativeProfitCutOff: BigDecimal = 0.002.toBigDecimal(),
    /**
     * It does not make sense to expose too big opportunities, that means opportunity would not be used anyway (e.g. problem with transfer, trading stopped)
     */
    private val maxRelativeProfitCutOff: BigDecimal = BigDecimal("1.0"),
) {
    private val minUsd24hVolume = 1000.toBigDecimal()
    private val plusInfinity = Long.MAX_VALUE.toBigDecimal()

    private fun TwoLegArbitrageProfitOpportunityAtDepth.toDto(shouldHideOpportunityDetails: Boolean): TwoLegArbitrageProfitOpportunityAtDepthDto? {
        return if (relativeProfit in minRelativeProfitCutOff..maxRelativeProfitCutOff) {
            TwoLegArbitrageProfitOpportunityAtDepthDto(
                sellPrice = if (shouldHideOpportunityDetails) null else sellPrice.setScale(8, HALF_EVEN).toPlainString(),
                sellAmount = if (shouldHideOpportunityDetails) null else baseCurrencyAmountAtSellExchange.setScale(8, HALF_EVEN).toPlainString(),
                buyPrice = buyPrice.setScale(8, HALF_EVEN).toPlainString(),
                buyAmount = baseCurrencyAmountAtBuyExchange.setScale(8, HALF_EVEN).toPlainString(),
                relativeProfitPercent = relativeProfit.movePointRight(2).setScale(4, HALF_EVEN).toPlainString(),
                profitUsd = profitUsd.setScale(2, HALF_EVEN).toPlainString(),
                usdDepthUpTo = usdDepthUpTo.setScale(2, HALF_DOWN).toPlainString(),
                fees = TwoLegArbitrageProfitOpportunityFeesDto(
                    buyFee = this.transactionFeeAmountBeforeTransfer?.setScale(8, HALF_EVEN)?.toPlainString(),
                    isDefaultBuyFeeUsed = this.isDefaultTransactionFeeAmountBeforeTransferUsed,
                    withdrawalFee = this.transferFeeAmount?.setScale(8, HALF_EVEN)?.toPlainString(),
                    sellFee = this.transactionFeeAmountAfterTransfer?.setScale(8, HALF_EVEN)?.toPlainString(),
                    isDefaultSellFeeUsed = this.isDefaultTransactionFeeAmountAfterTransferUsed,
                )
            )
        } else {
            null
        }
    }

    private fun TwoLegArbitrageProfitOpportunity.toDto(shouldHideOpportunityDetails: Boolean) = TwoLegArbitrageProfitOpportunityDto(
        baseCurrency = currencyPairWithExchangePair.currencyPair.base,
        counterCurrency = currencyPairWithExchangePair.currencyPair.counter,
        buyAtExchange = buyAtExchange,
        sellAtExchange = if (shouldHideOpportunityDetails) null else sellAtExchange,
        usd24hVolumeAtBuyExchange = usd24hVolumeAtFirstExchange.setScale(2, HALF_DOWN).toPlainString(),
        usd24hVolumeAtSellExchange = if (shouldHideOpportunityDetails) null else usd24hVolumeAtSecondExchange.setScale(2, HALF_DOWN).toPlainString(),
        profitOpportunityHistogram = profitOpportunityHistogram.map {
            it?.toDto(shouldHideOpportunityDetails)
        },
        areDetailsHidden = shouldHideOpportunityDetails,
        calculatedAtMillis = calculatedAtMillis
    )

    fun process(allProfits: Sequence<TwoLegArbitrageProfitOpportunity>, isUserInProPlan: Boolean): List<TwoLegArbitrageProfitOpportunityDto> {
        return allProfits.mapNotNull { profit ->
            val anyDepthHasProfitBetweenMinAndMax =
                profit.profitOpportunityHistogram
                    .any { opportunity ->
                        (opportunity?.relativeProfit ?: ZERO) > minRelativeProfitCutOff
                                && (opportunity?.relativeProfit ?: plusInfinity) < maxRelativeProfitCutOff
                    }
            if (anyDepthHasProfitBetweenMinAndMax && profit.minUsd24hVolumeOfBothExchanges > minUsd24hVolume) {
                profit.toDto(
                    shouldHideOpportunityDetails = !isUserInProPlan
                            && profit.profitOpportunityHistogram.any {
                        (it?.relativeProfit ?: ZERO) > freePlanRelativeProfitCutOff
                    }
                )
            } else null
        }
            .toList()
    }

}

class ArbitrageProfitController(
    private val twoLegArbitrageProfitOpportunityCache: TwoLegArbitrageProfitOpportunityCache,
    private val orderBookUsdAmountThresholds: List<BigDecimal>,
    private val commonExchangeCurrencyPairsService: CommonExchangeCurrencyPairsService,
    private val objectMapper: ObjectMapper,
    private val oauth2BearerTokenAuthHandlerWrapper: HttpHandlerWrapper,
    private val isUserInProPlanFunction: (httpServerExchange: HttpServerExchange) -> Boolean = { it -> it.securityContext.authenticatedUserHasRole("ROLE_PRO_USER") },
    private val clientTwoLegArbitrageProfitOpportunities: ClientTwoLegArbitrageProfitOpportunities,
    private val freePlanRelativeProfitPercentCutOff: String,
    private val transactionFeeRatioWhenNotAvailableInMetadata: BigDecimal,
) : ApiController {

    private fun getTwoLegArbitrageProfits() = object : ApiHandler {
        override val method = GET
        override val urlTemplate = "/two-leg-arbitrage-profits"

        override val httpHandler = HttpHandler { httpServerExchange ->
            val isUserInProPlan = isUserInProPlanFunction(httpServerExchange)
            val profits = clientTwoLegArbitrageProfitOpportunities.process(twoLegArbitrageProfitOpportunityCache.getAllProfits(), isUserInProPlan)

            val result = TwoLegArbitrageProfitOpportunitiesResponseDto(
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
                isIncludingProPlanOpportunities = isUserInProPlan,
                defaultTransactionFeePercent = transactionFeeRatioWhenNotAvailableInMetadata.movePointRight(2).toPlainString(),
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
