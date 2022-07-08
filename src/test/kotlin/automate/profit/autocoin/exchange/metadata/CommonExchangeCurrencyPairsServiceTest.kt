package automate.profit.autocoin.exchange.metadata

import automate.profit.autocoin.app.ExchangePair
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

    private val exchange1WithFailedMetadataResponse = GATEIO
    private val exchange2WithFailedMetadataResponse = CEXIO
    private val exchanges = listOf(BINANCE, exchange1WithFailedMetadataResponse, BITTREX, KUCOIN, exchange2WithFailedMetadataResponse)

    private val bittrexMetadata = ExchangeMetadata(
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
        currencyMetadata = emptyMap(),
        currencyPairMetadata = emptyMap(),
        debugWarnings = emptyList()
    )

    private val exchangeMetadataService = mock<ExchangeMetadataService>().apply {
        whenever(this.getMetadata("bittrex")).thenReturn(bittrexMetadata)
        whenever(this.getMetadata("kucoin")).thenReturn(kucoinMetadata)
        whenever(this.getMetadata("binance")).thenReturn(binanceMetadata)
        whenever(this.getMetadata(exchange1WithFailedMetadataResponse.exchangeName)).thenThrow(RuntimeException("Exchange 1 failing on purpose"))
        whenever(this.getMetadata(exchange2WithFailedMetadataResponse.exchangeName)).thenThrow(RuntimeException("Exchange 2 failing on purpose"))
    }

    @Test
    fun shouldNotContainCurrenciesThatExistAtOneExchangeOnly() {
        // given
        val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(exchangeMetadataService, exchanges)
        // when
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.calculateCommonCurrencyPairs().currencyPairsToExchangePairs
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
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.calculateCommonCurrencyPairs().currencyPairsToExchangePairs
        // then
        assertThat(commonCurrencyPairs.getValue(commonForBittrexAndKucoin)).containsOnly(ExchangePair(BITTREX, KUCOIN))
        assertThat(commonCurrencyPairs.getValue(commonForBittrexAndBinance)).containsOnly(ExchangePair(BINANCE, BITTREX))
        assertThat(commonCurrencyPairs.getValue(commonForKucoinAndBinance)).containsOnly(ExchangePair(BINANCE, KUCOIN))
    }

    @Test
    fun shouldCalculateExchangeToCurrencyPairsCommonForAtLeastOneOtherExchange() {
        // given
        val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(exchangeMetadataService, exchanges)
        // when
        val bittrexCurrencyPairs = commonExchangeCurrencyPairsService.calculateCommonCurrencyPairs().exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange
        // then
        assertThat(bittrexCurrencyPairs.getValue(BITTREX)).containsExactly(commonForAllExchanges, commonForBittrexAndBinance, commonForBittrexAndKucoin)
    }

    @Test
    fun shouldContainCurrenciesThatExistAtAllExchanges() {
        // given
        val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(exchangeMetadataService, exchanges)
        // when
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.calculateCommonCurrencyPairs().currencyPairsToExchangePairs
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
            whenever(this.getMetadata(exchange1WithFailedMetadataResponse.exchangeName)).thenThrow(RuntimeException("Exchange 1 failing on purpose"))
            whenever(this.getMetadata(exchange2WithFailedMetadataResponse.exchangeName)).thenThrow(RuntimeException("Exchange 2 failing on purpose"))
        }
        val commonExchangeCurrencyPairsService = CommonExchangeCurrencyPairsService(exchangeMetadataService, exchanges)
        // when
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.calculateCommonCurrencyPairs().currencyPairsToExchangePairs
        // then
        assertThat(commonCurrencyPairs).isEmpty()
    }

}
