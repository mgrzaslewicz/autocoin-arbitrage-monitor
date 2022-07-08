package automate.profit.autocoin.exchange.metadata

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class CommonExchangeCurrencyPairsServiceTest {
    private val doesNotMatter = mock<CurrencyPairMetadata>()

    private val commonForAllExchanges = CurrencyPair.of("LSK/ETH")
    private val commonForBittrexAndBinance = CurrencyPair.of("LSK/XRP")
    private val commonForBittrexAndKucoin = CurrencyPair.of("LSK/BTC")
    private val commonForKucoinAndBinance = CurrencyPair.of("EOS/ETH")

    private val onlyAtBittrex = CurrencyPair.of("ONLY/ATBITTREX")
    private val onlyAtBinance = CurrencyPair.of("ONLY/ATBINANCE")
    private val onlyAtKucoin = CurrencyPair.of("ONLY/ATKUCOIN")

    private val exchanges = listOf(BINANCE, BITTREX, KUCOIN)

    private val bittrexMetadata = ExchangeMetadata(
            currencyMetadata = emptyMap(),
            currencyPairMetadata = mapOf(
                    commonForAllExchanges to doesNotMatter,
                    commonForBittrexAndBinance to doesNotMatter,
                    commonForBittrexAndKucoin to doesNotMatter,
                    onlyAtBittrex to doesNotMatter
            )
    )
    private val binanceMetadata = ExchangeMetadata(
            currencyMetadata = emptyMap(),
            currencyPairMetadata = mapOf(
                    commonForAllExchanges to doesNotMatter,
                    commonForBittrexAndBinance to doesNotMatter,
                    commonForKucoinAndBinance to doesNotMatter,
                    onlyAtBinance to doesNotMatter
            )
    )
    private val kucoinMetadata = ExchangeMetadata(
            currencyMetadata = emptyMap(),
            currencyPairMetadata = mapOf(
                    commonForAllExchanges to doesNotMatter,
                    commonForKucoinAndBinance to doesNotMatter,
                    commonForBittrexAndKucoin to doesNotMatter,
                    onlyAtKucoin to doesNotMatter
            )
    )
    private val emptyMetadata = ExchangeMetadata(
            currencyMetadata = emptyMap(),
            currencyPairMetadata = emptyMap()
    )

    private val exchangeMetadataService = mock<ExchangeMetadataService>().apply {
        whenever(this.getMetadata("bittrex")).thenReturn(bittrexMetadata)
        whenever(this.getMetadata("kucoin")).thenReturn(kucoinMetadata)
        whenever(this.getMetadata("binance")).thenReturn(binanceMetadata)
    }

    @Test
    fun shouldNotContainCurrenciesThatExistAtOneExchangeOnly() {
        // given
        val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(exchangeMetadataService, exchanges)
        // when
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.getCommonCurrencyPairs()
        // then
        assertThat(commonCurrencyPairs).doesNotContainKey(onlyAtBinance)
        assertThat(commonCurrencyPairs).doesNotContainKey(onlyAtBittrex)
        assertThat(commonCurrencyPairs).doesNotContainKey(onlyAtKucoin)
    }

    @Test
    fun shouldContainCurrenciesThatExistAtTwoExchangesOnly() {
        // given
        val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(exchangeMetadataService, exchanges)
        // when
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.getCommonCurrencyPairs()
        // then
        assertThat(commonCurrencyPairs.getValue(commonForBittrexAndKucoin)).containsOnly(ExchangePair(BITTREX, KUCOIN))
        assertThat(commonCurrencyPairs.getValue(commonForBittrexAndBinance)).containsOnly(ExchangePair(BINANCE, BITTREX))
        assertThat(commonCurrencyPairs.getValue(commonForKucoinAndBinance)).containsOnly(ExchangePair(BINANCE, KUCOIN))
    }

    @Test
    fun shouldContainCurrenciesThatExistAtAllExchanges() {
        // given
        val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(exchangeMetadataService, exchanges)
        // when
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.getCommonCurrencyPairs()
        // then
        assertThat(commonCurrencyPairs.getValue(commonForAllExchanges))
                .containsOnly(
                        ExchangePair(BITTREX, KUCOIN),
                        ExchangePair(BINANCE, BITTREX),
                        ExchangePair(BINANCE, KUCOIN)
                )
    }

    @Test
    fun shouldReturnEmpty() {
        // given
        val exchangeMetadataService = mock<ExchangeMetadataService>().apply {
            whenever(this.getMetadata("bittrex")).thenReturn(bittrexMetadata)
            whenever(this.getMetadata("kucoin")).thenReturn(emptyMetadata)
            whenever(this.getMetadata("binance")).thenReturn(emptyMetadata)
        }
        val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(exchangeMetadataService, exchanges)
        // when
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.getCommonCurrencyPairs()
        // then
        assertThat(commonCurrencyPairs).isEmpty()
    }

}