package automate.profit.autocoin.exchange.arbitrage

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import java.math.BigDecimal

data class TwoLegArbitrageProfit(
        val currencyPair: CurrencyPair,
        val exchangePair: ExchangePair,
        val sellPrice: BigDecimal,
        val buyPrice: BigDecimal,
        val sellAtExchange: SupportedExchange,
        val usd24hVolumeAtSellExchange: BigDecimal,
        val buyAtExchange: SupportedExchange,
        val usd24hVolumeAtBuyExchange: BigDecimal,
        val relativeProfit: BigDecimal,
        val calculatedAtMillis: Long
) {

    val currencyPairWithExchangePair: CurrencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)

    val minUsd24hVolumeOfBothExchanges = usd24hVolumeAtBuyExchange.min(usd24hVolumeAtSellExchange)

    override fun toString(): String {
        return "TwoLegArbitrageProfit(" +
                "currencyPair=$currencyPair, " +
                "exchangePair=$exchangePair, " +
                "sellPrice=$sellPrice, " +
                "buyPrice=$buyPrice, " +
                "sellAtExchange=$sellAtExchange, " +
                "usd24hVolumeAtSellExchange=$usd24hVolumeAtSellExchange, " +
                "buyAtExchange=$buyAtExchange, " +
                "usd24hVolumeAtBuyExchange=$usd24hVolumeAtBuyExchange, " +
                "relativeProfit=${relativeProfit.multiply(BigDecimal.valueOf(100))} %), " +
                "calculatedAtMillis=$calculatedAtMillis, " +
                "currencyPairWithExchangePair=$currencyPairWithExchangePair" +
                ")"
    }

}