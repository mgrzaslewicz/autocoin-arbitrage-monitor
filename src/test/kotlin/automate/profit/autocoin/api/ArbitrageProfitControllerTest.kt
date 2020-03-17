package automate.profit.autocoin.api

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.config.ObjectMapperProvider
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairs
import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.metrics.MetricsService
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import io.undertow.server.HttpHandler
import me.alexpanov.net.FreePortFinder
import okhttp3.OkHttpClient
import okhttp3.Request
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import java.net.ServerSocket


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
                                    CurrencyPair.of("ABC/DEF") to setOf(ExchangePair(
                                            firstExchange = SupportedExchange.BITTREX,
                                            secondExchange = SupportedExchange.BINANCE
                                    ))
                            ),
                            exchangePairsToCurrencyPairs = emptyMap()
                    )
            )
        }
        val arbitageProfitController = ArbitrageProfitController(
                twoLegOrderBookArbitrageProfitCache = mock(),
                orderBookUsdAmountThresholds = listOf(),
                objectMapper = objectMapper,
                oauth2BearerTokenAuthHandlerWrapper = NoopHttpHandlerWrapper(),
                commonExchangeCurrencyPairsService = commonExchangeCurrencyPairsService
        )
        val serverBuilder = ServerBuilder(
                appServerPort = getFreePort(),
                apiControllers = listOf(arbitageProfitController),
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
            assertThat(metadataDto).isEqualTo(TwoLegArbitrageMetadataDto(
                    baseCurrenciesMonitored = setOf("ABC"),
                    counterCurrenciesMonitored = setOf("DEF")
            ))
        }
        server.stop()
    }
}