package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import com.autocoin.exchangegateway.spi.exchange.Exchange
import java.math.BigDecimal

data class TwoLegArbitrageProfitOpportunityAtDepth(
    val sellPrice: BigDecimal,
    val baseCurrencyAmountAtSellExchange: BigDecimal,
    val buyPrice: BigDecimal,
    val baseCurrencyAmountAtBuyExchange: BigDecimal,
    val relativeProfit: BigDecimal,
    val profitUsd: BigDecimal,
    val usdDepthUpTo: BigDecimal,
    val transactionFeeAmountBeforeTransfer: BigDecimal?,
    val transferFeeAmount: BigDecimal?,
    val transactionFeeAmountAfterTransfer: BigDecimal?,
    val isDefaultTransactionFeeAmountBeforeTransferUsed: Boolean,
    val isDefaultTransactionFeeAmountAfterTransferUsed: Boolean,
)

data class TwoLegArbitrageProfitOpportunity(
    val buyAtExchange: Exchange,
    val sellAtExchange: Exchange,
    val currencyPairWithExchangePair: CurrencyPairWithExchangePair,
    val usd24hVolumeAtBuyExchange: BigDecimal?,
    val usd24hVolumeAtSellExchange: BigDecimal?,
    val profitOpportunityHistogram: List<TwoLegArbitrageProfitOpportunityAtDepth?>,
    val calculatedAtMillis: Long,
    val olderOrderBookReceivedAtOrExchangeMillis: Long,
) {


}
