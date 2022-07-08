package automate.profit.autocoin.api

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.BINANCE
import automate.profit.autocoin.exchange.SupportedExchange.BITTREX
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunityAtDepth
import automate.profit.autocoin.exchange.arbitrage.orderbook.TwoLegArbitrageProfitOpportunity
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.SoftAssertions
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class ClientTwoLegArbitrageProfitOpportunitiesTest {

    private val opportunitiesToProcess = listOf(
        TwoLegArbitrageProfitOpportunity(
            buyAtExchange = BITTREX,
            sellAtExchange = BINANCE,
            currencyPairWithExchangePair = CurrencyPairWithExchangePair(
                exchangePair = ExchangePair(BINANCE, BITTREX),
                currencyPair = CurrencyPair.of("ETH/BTC")
            ),
            usd24hVolumeAtFirstExchange = BigDecimal("12000"),
            usd24hVolumeAtSecondExchange = BigDecimal("13000"),
            profitOpportunityHistogram = listOf(
                TwoLegArbitrageProfitOpportunityAtDepth(
                    sellPrice = BigDecimal.ONE,
                    baseCurrencyAmountAtSellExchange = BigDecimal("101"),
                    buyPrice = BigDecimal("1.1"),
                    baseCurrencyAmountAtBuyExchange = BigDecimal("103"),
                    relativeProfit = BigDecimal("0.01"),
                    profitUsd = BigDecimal("95"),
                    usdDepthUpTo = BigDecimal("15000"),
                    transactionFeeAmountBeforeTransfer = BigDecimal("0.21"),
                    isDefaultTransactionFeeAmountBeforeTransferUsed = true,
                    transferFeeAmount = BigDecimal("0.3"),
                    transactionFeeAmountAfterTransfer = BigDecimal("0.6"),
                    isDefaultTransactionFeeAmountAfterTransferUsed = false,
                )
            ),
            calculatedAtMillis = 15L,
        )
    )

    @Test
    fun shouldNotHideOpportunityDetails() {
        // given
        val tested = ClientTwoLegArbitrageProfitOpportunities(freePlanRelativeProfitCutOff = BigDecimal("0.02"))
        // when
        val result = tested.process(
            allProfits = opportunitiesToProcess.asSequence(),
            isUserInProPlan = true
        )
        // then
        assertThat(result).hasSize(1)
        SoftAssertions().apply {
            assertThat(result.first().buyAtExchange).isEqualTo(BITTREX)
            assertThat(result.first().sellAtExchange).isEqualTo(BINANCE)
            assertThat(result.first().baseCurrency).isEqualTo("ETH")
            assertThat(result.first().counterCurrency).isEqualTo("BTC")
            assertThat(result.first().usd24hVolumeAtBuyExchange).isEqualTo("12000.00")
            assertThat(result.first().usd24hVolumeAtSellExchange).isEqualTo("13000.00")
            assertThat(result.first().calculatedAtMillis).isEqualTo(15L)
            assertThat(result.first().profitOpportunityHistogram).hasSize(1)
            assertThat(result.first().profitOpportunityHistogram.first()!!.sellPrice).isEqualTo("1.00000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.sellAmount).isEqualTo("101.00000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.buyPrice).isEqualTo("1.10000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.buyAmount).isEqualTo("103.00000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.relativeProfitPercent).isEqualTo("1.0000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.profitUsd).isEqualTo("95.00")
            assertThat(result.first().profitOpportunityHistogram.first()!!.usdDepthUpTo).isEqualTo("15000.00")
            assertThat(result.first().profitOpportunityHistogram.first()!!.fees.buyFee).isEqualTo("0.21000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.fees.isBuyFeeEstimated).isTrue
            assertThat(result.first().profitOpportunityHistogram.first()!!.fees.withdrawalFee).isEqualTo("0.30000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.fees.sellFee).isEqualTo("0.60000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.fees.isSellFeeEstimated).isFalse
            assertAll()
        }
    }

    @Test
    fun shouldHideOpportunityDetails() {
        // given
        val tested = ClientTwoLegArbitrageProfitOpportunities(freePlanRelativeProfitCutOff = BigDecimal("0.009"))
        // when
        val result = tested.process(
            allProfits = opportunitiesToProcess.asSequence(),
            isUserInProPlan = false
        )
        // then
        assertThat(result).hasSize(1)
        SoftAssertions().apply {
            assertThat(result.first().buyAtExchange).isEqualTo(BITTREX)
            assertThat(result.first().sellAtExchange).isNull()
            assertThat(result.first().baseCurrency).isEqualTo("ETH")
            assertThat(result.first().counterCurrency).isEqualTo("BTC")
            assertThat(result.first().usd24hVolumeAtBuyExchange).isEqualTo("12000.00")
            assertThat(result.first().usd24hVolumeAtSellExchange).isNull()
            assertThat(result.first().calculatedAtMillis).isEqualTo(15L)
            assertThat(result.first().profitOpportunityHistogram).hasSize(1)
            assertThat(result.first().profitOpportunityHistogram.first()!!.sellPrice).isNull()
            assertThat(result.first().profitOpportunityHistogram.first()!!.sellAmount).isNull()
            assertThat(result.first().profitOpportunityHistogram.first()!!.buyPrice).isEqualTo("1.10000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.buyAmount).isEqualTo("103.00000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.relativeProfitPercent).isEqualTo("1.0000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.profitUsd).isEqualTo("95.00")
            assertThat(result.first().profitOpportunityHistogram.first()!!.usdDepthUpTo).isEqualTo("15000.00")
            assertThat(result.first().profitOpportunityHistogram.first()!!.fees.buyFee).isEqualTo("0.21000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.fees.isBuyFeeEstimated).isTrue
            assertThat(result.first().profitOpportunityHistogram.first()!!.fees.withdrawalFee).isEqualTo("0.30000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.fees.sellFee).isEqualTo("0.60000000")
            assertThat(result.first().profitOpportunityHistogram.first()!!.fees.isSellFeeEstimated).isFalse
            assertAll()
        }
    }

}
