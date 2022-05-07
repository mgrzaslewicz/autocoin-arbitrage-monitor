package automate.profit.autocoin.api

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.config.ObjectMapperProvider
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegOrderBookArbitrageProfitOpportunityCache
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
            twoLegOrderBookArbitrageProfitOpportunityCache = mock(),
            orderBookUsdAmountThresholds = listOf(),
            objectMapper = objectMapper,
            oauth2BearerTokenAuthHandlerWrapper = NoopHttpHandlerWrapper(),
            commonExchangeCurrencyPairsService = commonExchangeCurrencyPairsService,
            clientTwoLegArbitrageProfits = ClientTwoLegArbitrageProfits(BigDecimal("0.01")),
            freePlanRelativeProfitPercentCutOff = "0.01",
            isUserInProPlanFunction = { _ -> false },
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
                )
            )
        }
        server.stop()
    }

    @Test
    fun shouldSendArbitrageOpportunities() {
        // given
        val twoLegOrderBookArbitrageProfitOpportunityCache = mock<TwoLegOrderBookArbitrageProfitOpportunityCache>().apply {
            whenever(this.getAllProfits()).thenReturn(mock())
        }
        val arbitrageProfitController = ArbitrageProfitController(
            twoLegOrderBookArbitrageProfitOpportunityCache = twoLegOrderBookArbitrageProfitOpportunityCache,
            orderBookUsdAmountThresholds = listOf(),
            objectMapper = objectMapper,
            oauth2BearerTokenAuthHandlerWrapper = NoopHttpHandlerWrapper(),
            commonExchangeCurrencyPairsService = mock(),
            isUserInProPlanFunction = { _ -> true },
            clientTwoLegArbitrageProfits = mock<ClientTwoLegArbitrageProfits>().apply {
                whenever(this.process(any(), eq(true))).thenReturn(
                    listOf(
                        TwoLegArbitrageProfitDto(
                            baseCurrency = "A",
                            counterCurrency = "B",
                            firstExchange = BINANCE,
                            secondExchange = BITTREX,
                            usd24hVolumeAtFirstExchange = "12000.00",
                            usd24hVolumeAtSecondExchange = "13000.00",
                            arbitrageProfitHistogram = listOf(
                                TwoLegOrderBookArbitrageOpportunityDto(
                                    sellPrice = "0.15",
                                    sellAmount = "21",
                                    sellAtExchange = BINANCE,
                                    buyPrice = "0.3",
                                    buyAmount = "18",
                                    buyAtExchange = BITTREX,
                                    relativeProfitPercent = "0.015",
                                    profitUsd = "5.2",
                                    usdDepthUpTo = "15000",
                                    areDetailsHidden = false,
                                    fees = TwoLegOrderBookArbitrageOpportunityFeesDto(
                                        buyFee = "0.5",
                                        withdrawalFee = "0.1",
                                        sellFee = "0.2",
                                    )
                                )
                            ),
                            calculatedAtMillis = 15L,
                        )
                    )
                )
            },
            freePlanRelativeProfitPercentCutOff = "0.03",
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
            val twoLegArbitrageResponseDto = objectMapper.readValue(it.body?.string(), TwoLegArbitrageResponseDto::class.java)
            assertThat(twoLegArbitrageResponseDto.profits).hasSize(1)
            twoLegArbitrageResponseDto.profits.first()!!.apply {
                SoftAssertions().apply {
                    assertThat(baseCurrency).isEqualTo("A")
                    assertThat(counterCurrency).isEqualTo("B")
                    assertThat(firstExchange).isEqualTo(BINANCE)
                    assertThat(secondExchange).isEqualTo(BITTREX)
                    assertThat(usd24hVolumeAtFirstExchange).isEqualTo("12000.00")
                    assertThat(usd24hVolumeAtSecondExchange).isEqualTo("13000.00")
                    assertThat(arbitrageProfitHistogram).hasSize(1)
                    assertThat(arbitrageProfitHistogram.first()!!.sellPrice).isEqualTo("0.15")
                    assertThat(arbitrageProfitHistogram.first()!!.sellAmount).isEqualTo("21")
                    assertThat(arbitrageProfitHistogram.first()!!.sellAtExchange).isEqualTo("BINANCE")
                    assertThat(arbitrageProfitHistogram.first()!!.buyPrice).isEqualTo("0.3")
                    assertThat(arbitrageProfitHistogram.first()!!.buyAmount).isEqualTo("18")
                    assertThat(arbitrageProfitHistogram.first()!!.buyAtExchange).isEqualTo("BITTREX")
                    assertThat(arbitrageProfitHistogram.first()!!.relativeProfitPercent).isEqualTo("0.015")
                    assertThat(arbitrageProfitHistogram.first()!!.profitUsd).isEqualTo("5.5")
                    assertThat(arbitrageProfitHistogram.first()!!.usdDepthUpTo).isEqualTo("15000")
                    assertThat(arbitrageProfitHistogram.first()!!.areDetailsHidden).isFalse
                    assertThat(arbitrageProfitHistogram.first()!!.fees.buyFee).isEqualTo("0.5")
                    assertThat(arbitrageProfitHistogram.first()!!.fees.withdrawalFee).isEqualTo("0.1")
                    assertThat(arbitrageProfitHistogram.first()!!.fees.sellFee).isEqualTo("0.2")
                }
            }
        }
    }
}
