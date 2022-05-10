package automate.profit.autocoin.api

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.config.ObjectMapperProvider
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityCache
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairs
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.eq
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.undertow.server.HttpHandler
import me.alexpanov.net.FreePortFinder
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal


class ArbitrageProfitControllerTest {

    private fun getFreePort() = FreePortFinder.findFreeLocalPort()
    private val httpClientWithoutAuthorization = OkHttpClient()
    private val objectMapper = ObjectMapperProvider().createObjectMapper()

    class NoopHttpHandlerWrapper : HttpHandlerWrapper {
        override fun wrap(next: HttpHandler) = next
    }

    @Test
    fun shouldGetArbitrageMetadata() {
        // given
        val commonExchangeCurrencyPairsService = mock<CommonExchangeCurrencyPairsService>().apply {
            whenever(this.lastCalculatedCommonExchangeCurrencyPairs).thenReturn(
                CommonExchangeCurrencyPairs(
                    currencyPairsToExchangePairs = mapOf(
                        CurrencyPair.of("ABC/DEF") to setOf(
                            ExchangePair(
                                firstExchange = BITTREX,
                                secondExchange = BINANCE
                            )
                        )
                    ),
                    exchangePairsToCurrencyPairs = emptyMap()
                )
            )
        }
        val arbitrageProfitController = ArbitrageProfitController(
            twoLegArbitrageProfitOpportunityCache = mock(),
            orderBookUsdAmountThresholds = listOf(),
            objectMapper = objectMapper,
            oauth2BearerTokenAuthHandlerWrapper = NoopHttpHandlerWrapper(),
            commonExchangeCurrencyPairsService = commonExchangeCurrencyPairsService,
            clientTwoLegArbitrageProfitOpportunities = ClientTwoLegArbitrageProfitOpportunities(BigDecimal("0.01")),
            freePlanRelativeProfitPercentCutOff = "0.01",
            isUserInProPlanFunction = { false },
            transactionFeeRatioWhenNotAvailableInMetadata = BigDecimal("0.001"),
        )
        val serverBuilder = ServerBuilder(
            appServerPort = getFreePort(),
            apiControllers = listOf(arbitrageProfitController),
            metricsService = mock()
        )
        val server = serverBuilder.build()
        server.start()
        // when
        val request = Request.Builder()
            .url("http://localhost:${serverBuilder.appServerPort}/two-leg-arbitrage-metadata")
            .get()
            .build()
        val response = httpClientWithoutAuthorization.newCall(request).execute()
        // then
        response.use {
            val metadataDto = objectMapper.readValue(it.body?.string(), TwoLegArbitrageMetadataDto::class.java)
            assertThat(metadataDto).isEqualTo(
                TwoLegArbitrageMetadataDto(
                    baseCurrenciesMonitored = setOf("ABC"),
                    counterCurrenciesMonitored = setOf("DEF"),
                    freePlanProfitPercentCutOff = "0.01",
                    isIncludingProPlanOpportunities = false,
                    defaultTransactionFeePercent = "0.1",
                )
            )
        }
        server.stop()
    }

    @Test
    fun shouldGetArbitrageProfitOpportunities() {
        // given
        val twoLegArbitrageProfitOpportunityCache = mock<TwoLegArbitrageProfitOpportunityCache>().apply {
            whenever(this.getAllProfits()).thenReturn(mock())
        }
        val arbitrageProfitController = ArbitrageProfitController(
            twoLegArbitrageProfitOpportunityCache = twoLegArbitrageProfitOpportunityCache,
            orderBookUsdAmountThresholds = listOf(),
            objectMapper = objectMapper,
            oauth2BearerTokenAuthHandlerWrapper = NoopHttpHandlerWrapper(),
            commonExchangeCurrencyPairsService = mock(),
            isUserInProPlanFunction = { true },
            clientTwoLegArbitrageProfitOpportunities = mock<ClientTwoLegArbitrageProfitOpportunities>().apply {
                whenever(this.process(any(), eq(true))).thenReturn(
                    listOf(
                        TwoLegArbitrageProfitOpportunityDto(
                            baseCurrency = "A",
                            counterCurrency = "B",
                            buyAtExchange = BINANCE,
                            sellAtExchange = BITTREX,
                            usd24hVolumeAtBuyExchange = "12000.00",
                            usd24hVolumeAtSellExchange = "13000.00",
                            profitOpportunityHistogram = listOf(
                                TwoLegArbitrageProfitOpportunityAtDepthDto(
                                    sellPrice = "0.15",
                                    sellAmount = "21",
                                    buyPrice = "0.3",
                                    buyAmount = "18",
                                    relativeProfitPercent = "0.015",
                                    profitUsd = "5.2",
                                    usdDepthUpTo = "15000",
                                    fees = TwoLegArbitrageProfitOpportunityFeesDto(
                                        buyFee = "0.5",
                                        isDefaultBuyFeeUsed = false,
                                        withdrawalFee = "0.1",
                                        sellFee = "0.2",
                                        isDefaultSellFeeUsed = true,
                                    )
                                )
                            ),
                            areDetailsHidden = false,
                            calculatedAtMillis = 15L,
                        )
                    )
                )
            },
            freePlanRelativeProfitPercentCutOff = "0.03",
            transactionFeeRatioWhenNotAvailableInMetadata = BigDecimal("0.001"),
        )
        val serverBuilder = ServerBuilder(
            appServerPort = getFreePort(),
            apiControllers = listOf(arbitrageProfitController),
            metricsService = mock()
        )
        val server = serverBuilder.build()
        server.start()
        // when
        val request = Request.Builder()
            .url("http://localhost:${serverBuilder.appServerPort}/two-leg-arbitrage-profits")
            .get()
            .build()
        val response = httpClientWithoutAuthorization.newCall(request).execute()
        // then
        response.use {
            val twoLegArbitrageProfitOpportunitiesResponseDto = objectMapper.readValue(it.body?.string(), TwoLegArbitrageProfitOpportunitiesResponseDto::class.java)
            assertThat(twoLegArbitrageProfitOpportunitiesResponseDto.profits).hasSize(1)
            twoLegArbitrageProfitOpportunitiesResponseDto.profits.first()!!.apply {
                SoftAssertions().apply {
                    assertThat(baseCurrency).isEqualTo("A")
                    assertThat(counterCurrency).isEqualTo("B")
                    assertThat(buyAtExchange).isEqualTo(BINANCE)
                    assertThat(sellAtExchange).isEqualTo(BITTREX)
                    assertThat(usd24hVolumeAtBuyExchange).isEqualTo("12000.00")
                    assertThat(usd24hVolumeAtSellExchange).isEqualTo("13000.00")
                    assertThat(areDetailsHidden).isFalse
                    assertThat(profitOpportunityHistogram).hasSize(1)
                    assertThat(profitOpportunityHistogram.first()!!.sellPrice).isEqualTo("0.15")
                    assertThat(profitOpportunityHistogram.first()!!.sellAmount).isEqualTo("21")
                    assertThat(profitOpportunityHistogram.first()!!.buyPrice).isEqualTo("0.3")
                    assertThat(profitOpportunityHistogram.first()!!.buyAmount).isEqualTo("18")
                    assertThat(profitOpportunityHistogram.first()!!.relativeProfitPercent).isEqualTo("0.015")
                    assertThat(profitOpportunityHistogram.first()!!.profitUsd).isEqualTo("5.5")
                    assertThat(profitOpportunityHistogram.first()!!.usdDepthUpTo).isEqualTo("15000")
                    assertThat(profitOpportunityHistogram.first()!!.fees.buyFee).isEqualTo("0.5")
                    assertThat(profitOpportunityHistogram.first()!!.fees.isDefaultBuyFeeUsed).isFalse
                    assertThat(profitOpportunityHistogram.first()!!.fees.withdrawalFee).isEqualTo("0.1")
                    assertThat(profitOpportunityHistogram.first()!!.fees.sellFee).isEqualTo("0.2")
                    assertThat(profitOpportunityHistogram.first()!!.fees.isDefaultSellFeeUsed).isTrue
                }
            }
        }
    }
}
