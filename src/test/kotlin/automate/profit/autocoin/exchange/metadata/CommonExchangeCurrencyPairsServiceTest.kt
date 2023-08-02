package automate.profit.autocoin.exchange.metadata

import automate.profit.autocoin.TestExchange.exchangeA
import automate.profit.autocoin.TestExchange.exchangeB
import automate.profit.autocoin.TestExchange.exchangeC
import automate.profit.autocoin.app.config.ExchangePair
import com.autocoin.exchangegateway.api.exchange.currency.CurrencyPair
import com.autocoin.exchangegateway.api.exchange.metadata.ExchangeMetadata
import com.autocoin.exchangegateway.spi.exchange.metadata.CurrencyPairMetadata
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

    private val onlyAtBittrex = CurrencyPair.of("ONLY/AT-exchangeA")
    private val onlyAtBinance = CurrencyPair.of("ONLY/AT-exchangeB")
    private val onlyAtKucoin = CurrencyPair.of("ONLY/AT-exchangeC")

    private val bittrexMetadata = ExchangeMetadata(
        exchange = exchangeA,
        currencyMetadata = emptyMap(),
        currencyPairMetadata = mapOf(
            commonForAllExchanges to doesNotMatter,
            commonForBittrexAndBinance to doesNotMatter,
            commonForBittrexAndKucoin to doesNotMatter,
            onlyAtBittrex to doesNotMatter
        ),
        warnings = emptyList()
    )
    private val binanceMetadata = ExchangeMetadata(
        exchange = exchangeB,
        currencyMetadata = emptyMap(),
        currencyPairMetadata = mapOf(
            commonForAllExchanges to doesNotMatter,
            commonForBittrexAndBinance to doesNotMatter,
            commonForKucoinAndBinance to doesNotMatter,
            onlyAtBinance to doesNotMatter
        ),
        warnings = emptyList()
    )
    private val kucoinMetadata = ExchangeMetadata(
        exchange = exchangeC,
        currencyMetadata = emptyMap(),
        currencyPairMetadata = mapOf(
            commonForAllExchanges to doesNotMatter,
            commonForKucoinAndBinance to doesNotMatter,
            commonForBittrexAndKucoin to doesNotMatter,
            onlyAtKucoin to doesNotMatter
        ),
        warnings = emptyList()
    )
    private val emptyMetadata = ExchangeMetadata(
        exchange = exchangeC,
        currencyMetadata = emptyMap(),
        currencyPairMetadata = emptyMap(),
        warnings = emptyList()
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
        assertThat(commonCurrencyPairs.getValue(commonForBittrexAndKucoin)).containsOnly(
            ExchangePair(
                exchangeA,
                exchangeC
            )
        )
        assertThat(commonCurrencyPairs.getValue(commonForBittrexAndBinance)).containsOnly(
            ExchangePair(
                exchangeB,
                exchangeA
            )
        )
        assertThat(commonCurrencyPairs.getValue(commonForKucoinAndBinance)).containsOnly(
            ExchangePair(
                exchangeB,
                exchangeC
            )
        )
    }

    @Test
    fun shouldCalculateExchangeToCurrencyPairsCommonForAtLeastOneOtherExchange() {
        // when
        val bittrexCurrencyPairs = tested.calculateCommonCurrencyPairs().exchangeToCurrencyPairsCommonWithAtLeastOneOtherExchange
        // then
        assertThat(bittrexCurrencyPairs.getValue(exchangeA)).containsExactly(
            commonForAllExchanges,
            commonForBittrexAndBinance,
            commonForBittrexAndKucoin
        )
    }

    @Test
    fun shouldContainCurrenciesThatExistAtAllExchanges() {
        // when
        val commonCurrencyPairs = tested.calculateCommonCurrencyPairs().currencyPairsToExchangePairs
        // then
        assertThat(commonCurrencyPairs.getValue(commonForAllExchanges))
            .containsOnly(
                ExchangePair(exchangeA, exchangeC),
                ExchangePair(exchangeB, exchangeA),
                ExchangePair(exchangeB, exchangeC)
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
