package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import java.math.BigDecimal

data class TwoLegOrderBookArbitrageOpportunity(
        val sellPrice: BigDecimal,
        val sellAtExchange: SupportedExchange,
        val baseCurrencyAmountAtSellExchange: BigDecimal,
        val buyPrice: BigDecimal,
        val buyAtExchange: SupportedExchange,
        val baseCurrencyAmountAtBuyExchange: BigDecimal,
        val relativeProfit: BigDecimal,
        val usdDepthUpTo: BigDecimal,
        val transactionFeeAmountBeforeTransfer: BigDecimal?,
        val transferFeeAmount: BigDecimal?,
        val transactionFeeAmountAfterTransfer: BigDecimal?,
)

data class TwoLegOrderBookArbitrageProfit(
        val currencyPairWithExchangePair: CurrencyPairWithExchangePair,
        val usd24hVolumeAtFirstExchange: BigDecimal,
        val usd24hVolumeAtSecondExchange: BigDecimal,
        val orderBookArbitrageProfitHistogram: List<TwoLegOrderBookArbitrageOpportunity?>,
        val calculatedAtMillis: Long,
) {

    val minUsd24hVolumeOfBothExchanges = usd24hVolumeAtFirstExchange.min(usd24hVolumeAtSecondExchange)

}
