package automate.profit.autocoin.exchange.arbitrage.orderbook

import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.math.BigDecimal

class TwoLegArbitrageProfitOpportunityCutOffTest {

    @ParameterizedTest
    @CsvSource(
        "0.002,1.0,0.5,true",
        "0.002,1.0,0.002,true",
        "0.002,1.0,1.0,true",
        "0.002,1.0,1.01,false",
        "0.002,1.0,0.0019,false",
    )
    fun shouldProfitBeInAllowedRange(minRelativeProfitCutOff: BigDecimal, maxRelativeProfitCutOff: BigDecimal, relativeProfit: BigDecimal, expected: Boolean) {
        // given
        val tested = TwoLegArbitrageProfitOpportunityCutOff(
            minRelativeProfitCutOff = minRelativeProfitCutOff,
            maxRelativeProfitCutOff = maxRelativeProfitCutOff,
        )
        // when
        val isAllowed = tested.isRelativeProfitWithinAllowedRange(relativeProfit)
        // then
        assertThat(isAllowed).isEqualTo(expected)
    }

    @ParameterizedTest
    @CsvSource(
        "1000.0,1001,1000.5,false",
        "1000.0,1001,1000.0,false",
        "1000.0,,1000.0,false",
        "1000.0,1000.0,,false",
        "1000.0,1001,999.99,true",
        "1000.0,,999.99,true",
        "1000.0,999.99,,true",
    )
    fun shouldVolumeBeAboveMin(minUsd24hVolume: BigDecimal, usd24hVolumeAtFirstExchange: BigDecimal?, usd24hVolumeAtSecondExchange: BigDecimal?, expected: Boolean) {
        // given
        val tested = TwoLegArbitrageProfitOpportunityCutOff(minUsd24hVolume = minUsd24hVolume)
        // when
        val isTooLow = tested.isUsd24hVolumeTooLow(usd24hVolumeAtFirstExchange, usd24hVolumeAtSecondExchange)
        // then
        assertThat(isTooLow).isEqualTo(expected)
    }

}
