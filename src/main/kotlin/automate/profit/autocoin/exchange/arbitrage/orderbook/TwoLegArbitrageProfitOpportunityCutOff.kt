package automate.profit.autocoin.exchange.arbitrage.orderbook

import java.math.BigDecimal


/**
 * Decides when relativeProfit is too small or big and when exchange volume is too low.
 */
class TwoLegArbitrageProfitOpportunityCutOff(
    private val minRelativeProfitCutOff: BigDecimal = 0.002.toBigDecimal(),
    private val maxRelativeProfitCutOff: BigDecimal = BigDecimal("1.0"),
    private val minUsd24hVolume: BigDecimal = 1000.toBigDecimal(),
) {

    private val plusInfinity = Long.MAX_VALUE.toBigDecimal()

    /**
     * It does not make sense to expose opportunities with too small relative profit (like small fraction of a percent) as no one will use it.
     * The same goes for opportunities with too big relative profit, like hundreds of percent - it's always a false opportunity because of cryptocurrency name mismatch
     */
    fun isRelativeProfitWithinAllowedRange(relativeProfit: BigDecimal): Boolean {
        return relativeProfit in minRelativeProfitCutOff..maxRelativeProfitCutOff
    }

    fun isUsd24hVolumeTooLow(usd24hVolumeAtFirstExchange: BigDecimal?, usd24hVolumeAtSecondExchange: BigDecimal?): Boolean {
        return minUsd24hVolumeOfBothExchanges(usd24hVolumeAtFirstExchange, usd24hVolumeAtSecondExchange) ?: plusInfinity < minUsd24hVolume
    }

    private fun minUsd24hVolumeOfBothExchanges(usd24hVolumeAtFirstExchange: BigDecimal?, usd24hVolumeAtSecondExchange: BigDecimal?) =
        when {
            usd24hVolumeAtFirstExchange != null && usd24hVolumeAtSecondExchange != null -> usd24hVolumeAtFirstExchange.min(usd24hVolumeAtSecondExchange)
            usd24hVolumeAtFirstExchange != null && usd24hVolumeAtSecondExchange == null -> usd24hVolumeAtFirstExchange
            usd24hVolumeAtFirstExchange == null && usd24hVolumeAtSecondExchange != null -> usd24hVolumeAtSecondExchange
            else -> null
        }
}
