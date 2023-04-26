package automate.profit.autocoin.exchange.metadata

import automate.profit.autocoin.app.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class CommonExchangeCurrencyPairsServiceTest {
    private val doesNotMatter = mock<CurrencyPairMetadata>()

    private val commonForAllExchanges = CurrencyPair.of("LSK/ETH")
    private val commonForBittrexAndBinance = CurrencyPair.of("LSK/XRP")
    private val commonForBittrexAndKucoin = CurrencyPair.of("LSK/BTC")
    private val commonForKucoinAndBinance = CurrencyPair.of("EOS/ETH")

    private val onlyAtBittrex = CurrencyPair.of("ONLY/AT-BITTREX")
    private val onlyAtBinance = CurrencyPair.of("ONLY/AT-BINANCE")
    private val onlyAtKucoin = CurrencyPair.of("ONLY/AT-KUCOIN")

    private val bittrexMetadata = ExchangeMetadata(
        exchange = BITTREX,
        currencyMetadata = emptyMap(),
        currencyPairMetadata = mapOf(
            commonForAllExchanges to doesNotMatter,
            commonForBittrexAndBinance to doesNotMatter,
            commonForBittrexAndKucoin to doesNotMatter,
            onlyAtBittrex to doesNotMatter
        ),
        debugWarnings = emptyList()
    )
    private val binanceMetadata = ExchangeMetadata(
        exchange = BINANCE,
        currencyMetadata = emptyMap(),
        currencyPairMetadata = mapOf(
            commonForAllExchanges to doesNotMatter,
            commonForBittrexAndBinance to doesNotMatter,
            commonForKucoinAndBinance to doesNotMatter,
            onlyAtBinance to doesNotMatter
        ),
        debugWarnings = emptyList()
    )
    private val kucoinMetadata = ExchangeMetadata(
        exchange = KUCOIN,
        currencyMetadata = emptyMap(),
        currencyPairMetadata = mapOf(
            commonForAllExchanges to doesNotMatter,
            commonForKucoinAndBinance to doesNotMatter,
            commonForBittrexAndKucoin to doesNotMatter,
            onlyAtKucoin to doesNotMatter
        ),
        debugWarnings = emptyList()
    )
    private val emptyMetadata = ExchangeMetadata(
        exchange = KUCOIN,
        currencyMetadata = emptyMap(),
        currencyPairMetadata = emptyMap(),
        debugWarnings = emptyList()
    )

    private val exchangeMetadataService = mock<ExchangeMetadataService>().apply {
        whenever(this.getAllExchangesMetadata()).thenReturn(listOf(binanceMetadata, bittrexMetadata, kucoinMetadata))
    }
    private val tested = CommonExchangeCurrencyPairsService(exchangeMetadataService)


    @Test
    fun shouldNotContainCurrenciesThatExistAtOneExchangeOnly() {
        // when
        val commonCurrencyPairs = tested.calculateCommonCurrencyPairs().currencyPairsToExchangePairs
        // then
        assertThat(commonCurrencyPairs).doesNotContainKey(onlyAtBinance)
        assertThat(commonCurrencyPairs).doesNotContainKey(onlyAtBittrex)
        assertThat(commonCurrencyPairs).doesNotContainKey(onlyAtKucoin)
    }

    @Test
    fun shouldContainCurrenciesThatExistAtTwoExchangesOnly() {
        // when
        val commonCurrencyPairs = tested.calculateCommonCurrencyPairs().currencyPairsToExchangePairs
        // then
        assertThat(commonCurrencyPairs.getValue(commonForBittrexAndKucoin)).containsOnly(ExchangePair(BITTREX, KUCOIN))
        assertThat(commonCurrencyPairs.getValue(commonForBittrexAndBinance)).containsOnly(ExchangePair(BINANCE, BITTREX))
        assertThat(commonCurrencyPairs.getValue(commonForKucoinAndBinance)).containsOnly(ExchangePair(BINANCE, KUCOIN))
    }

    @Test
    fun shouldCalculateExchangeToCurrencyPairsCommonForAtLeastOneOtherExchange() {
        // when
        val bittrexCurrencyPairs = tested.calculateCommonCurrencyPairs().exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange
        // then
        assertThat(bittrexCurrencyPairs.getValue(BITTREX)).containsExactly(commonForAllExchanges, commonForBittrexAndBinance, commonForBittrexAndKucoin)
    }

    @Test
    fun shouldContainCurrenciesThatExistAtAllExchanges() {
        // when
        val commonCurrencyPairs = tested.calculateCommonCurrencyPairs().currencyPairsToExchangePairs
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
        whenever(exchangeMetadataService.getAllExchangesMetadata()).thenReturn(listOf(bittrexMetadata, emptyMetadata, emptyMetadata))
        // when
        val commonCurrencyPairs = tested.calculateCommonCurrencyPairs().currencyPairsToExchangePairs
        // then
        assertThat(commonCurrencyPairs).isEmpty()
    }

}
