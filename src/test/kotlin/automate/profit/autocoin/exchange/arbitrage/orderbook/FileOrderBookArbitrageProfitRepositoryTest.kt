package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.config.ObjectMapperProvider
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal
import java.util.*

class FileOrderBookArbitrageProfitRepositoryTest {
    private val tempFolder = TemporaryFolder()
    private val currencyPair = CurrencyPair.of("A/B")
    private val exchangePair = ExchangePair(BITTREX, BINANCE)
    private val objectMapper = ObjectMapperProvider().createObjectMapper()
    private val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
    private val twoLegOrderBookArbitrageOpportunity = TwoLegOrderBookArbitrageOpportunity(
            sellPrice = BigDecimal("1.01"),
            sellAtExchange = BITTREX,
            baseCurrencyAmountAtSellExchange = BigDecimal("34.5"),
            buyPrice = BigDecimal("1.005"),
            buyAtExchange = BINANCE,
            baseCurrencyAmountAtBuyExchange = BigDecimal("35.0"),
            relativeProfit = BigDecimal("0.01"),
            usdDepthUpTo = BigDecimal("179.4")
    )
    private val sampleProfit1 = TwoLegOrderBookArbitrageProfit(
            currencyPairWithExchangePair = currencyPairWithExchangePair,
            usd24hVolumeAtFirstExchange = 1000.0.toBigDecimal(),
            usd24hVolumeAtSecondExchange = 1500.0.toBigDecimal(),
            orderBookArbitrageProfitHistogram = listOf(twoLegOrderBookArbitrageOpportunity),
            calculatedAtMillis = 1000
    )
    private val sampleProfit2 = TwoLegOrderBookArbitrageProfit(
            currencyPairWithExchangePair = currencyPairWithExchangePair,
            usd24hVolumeAtFirstExchange = 1001.0.toBigDecimal(),
            usd24hVolumeAtSecondExchange = 1501.0.toBigDecimal(),
            orderBookArbitrageProfitHistogram = listOf(twoLegOrderBookArbitrageOpportunity),
            calculatedAtMillis = 1005
    )
    private val sampleProfit3 = TwoLegOrderBookArbitrageProfit(
            currencyPairWithExchangePair = currencyPairWithExchangePair,
            usd24hVolumeAtFirstExchange = 1002.0.toBigDecimal(),
            usd24hVolumeAtSecondExchange = 1502.0.toBigDecimal(),
            orderBookArbitrageProfitHistogram = listOf(twoLegOrderBookArbitrageOpportunity),
            calculatedAtMillis = 1006
    )

    private val profitsToSave = listOf(sampleProfit1, sampleProfit2)
    private lateinit var arbitrageProfitRepository: FileOrderBookArbitrageProfitRepository
    private lateinit var profitsFolder: File

    @BeforeEach
    fun setup() {
        tempFolder.create()
        profitsFolder = tempFolder.newFolder()
        arbitrageProfitRepository = FileOrderBookArbitrageProfitRepository(profitsFolder.absolutePath, 100L, objectMapper) { 1L }
    }

    @AfterEach
    fun cleanup() {
        tempFolder.delete()
    }

    @Test
    fun shouldCreateDirectory() {
        // when
        arbitrageProfitRepository.saveAll(currencyPairWithExchangePair, profitsToSave)
        // then
        assertThat(profitsFolder).isDirectoryContaining { it.name == "bittrex-binance" }
    }

    @Test
    fun shouldCreateFile() {
        // when
        arbitrageProfitRepository.saveAll(currencyPairWithExchangePair, profitsToSave)
        // then
        assertThat(profitsFolder.resolve("bittrex-binance")).isDirectoryContaining { it.name == "A-B-bittrex-binance_19700101010000001.jsons" }
    }

    @Test
    fun shouldSaveProfitsToFile() {
        // given
        val expectedContent = """
{"currencyPairWithExchangePair":{"currencyPair":{"base":"A","counter":"B"},"exchangePair":{"firstExchange":"BITTREX","secondExchange":"BINANCE"}},"usd24hVolumeAtFirstExchange":1000.0,"usd24hVolumeAtSecondExchange":1500.0,"orderBookArbitrageProfitHistogram":[{"sellPrice":1.01,"sellAtExchange":"BITTREX","baseCurrencyAmountAtSellExchange":34.5,"buyPrice":1.005,"buyAtExchange":"BINANCE","baseCurrencyAmountAtBuyExchange":35.0,"relativeProfit":0.01,"usdDepthUpTo":179.4}],"calculatedAtMillis":1000,"minUsd24hVolumeOfBothExchanges":1000.0}
{"currencyPairWithExchangePair":{"currencyPair":{"base":"A","counter":"B"},"exchangePair":{"firstExchange":"BITTREX","secondExchange":"BINANCE"}},"usd24hVolumeAtFirstExchange":1001.0,"usd24hVolumeAtSecondExchange":1501.0,"orderBookArbitrageProfitHistogram":[{"sellPrice":1.01,"sellAtExchange":"BITTREX","baseCurrencyAmountAtSellExchange":34.5,"buyPrice":1.005,"buyAtExchange":"BINANCE","baseCurrencyAmountAtBuyExchange":35.0,"relativeProfit":0.01,"usdDepthUpTo":179.4}],"calculatedAtMillis":1005,"minUsd24hVolumeOfBothExchanges":1001.0}
        """.trimIndent()
        // when
        arbitrageProfitRepository.saveAll(currencyPairWithExchangePair, profitsToSave)
        // then
        assertThat(profitsFolder.resolve("bittrex-binance").resolve("A-B-bittrex-binance_19700101010000001.jsons")).hasContent(expectedContent)
    }

    @Test
    fun shouldAddProfitsToFile() {
        // given
        val expectedContent = """
{"currencyPairWithExchangePair":{"currencyPair":{"base":"A","counter":"B"},"exchangePair":{"firstExchange":"BITTREX","secondExchange":"BINANCE"}},"usd24hVolumeAtFirstExchange":1000.0,"usd24hVolumeAtSecondExchange":1500.0,"orderBookArbitrageProfitHistogram":[{"sellPrice":1.01,"sellAtExchange":"BITTREX","baseCurrencyAmountAtSellExchange":34.5,"buyPrice":1.005,"buyAtExchange":"BINANCE","baseCurrencyAmountAtBuyExchange":35.0,"relativeProfit":0.01,"usdDepthUpTo":179.4}],"calculatedAtMillis":1000,"minUsd24hVolumeOfBothExchanges":1000.0}
{"currencyPairWithExchangePair":{"currencyPair":{"base":"A","counter":"B"},"exchangePair":{"firstExchange":"BITTREX","secondExchange":"BINANCE"}},"usd24hVolumeAtFirstExchange":1001.0,"usd24hVolumeAtSecondExchange":1501.0,"orderBookArbitrageProfitHistogram":[{"sellPrice":1.01,"sellAtExchange":"BITTREX","baseCurrencyAmountAtSellExchange":34.5,"buyPrice":1.005,"buyAtExchange":"BINANCE","baseCurrencyAmountAtBuyExchange":35.0,"relativeProfit":0.01,"usdDepthUpTo":179.4}],"calculatedAtMillis":1005,"minUsd24hVolumeOfBothExchanges":1001.0}
{"currencyPairWithExchangePair":{"currencyPair":{"base":"A","counter":"B"},"exchangePair":{"firstExchange":"BITTREX","secondExchange":"BINANCE"}},"usd24hVolumeAtFirstExchange":1000.0,"usd24hVolumeAtSecondExchange":1500.0,"orderBookArbitrageProfitHistogram":[{"sellPrice":1.01,"sellAtExchange":"BITTREX","baseCurrencyAmountAtSellExchange":34.5,"buyPrice":1.005,"buyAtExchange":"BINANCE","baseCurrencyAmountAtBuyExchange":35.0,"relativeProfit":0.01,"usdDepthUpTo":179.4}],"calculatedAtMillis":1000,"minUsd24hVolumeOfBothExchanges":1000.0}
{"currencyPairWithExchangePair":{"currencyPair":{"base":"A","counter":"B"},"exchangePair":{"firstExchange":"BITTREX","secondExchange":"BINANCE"}},"usd24hVolumeAtFirstExchange":1001.0,"usd24hVolumeAtSecondExchange":1501.0,"orderBookArbitrageProfitHistogram":[{"sellPrice":1.01,"sellAtExchange":"BITTREX","baseCurrencyAmountAtSellExchange":34.5,"buyPrice":1.005,"buyAtExchange":"BINANCE","baseCurrencyAmountAtBuyExchange":35.0,"relativeProfit":0.01,"usdDepthUpTo":179.4}],"calculatedAtMillis":1005,"minUsd24hVolumeOfBothExchanges":1001.0}
        """.trimIndent()
        // when
        arbitrageProfitRepository.addAll(currencyPairWithExchangePair, profitsToSave)
        arbitrageProfitRepository.addAll(currencyPairWithExchangePair, profitsToSave)
        // then
        assertThat(profitsFolder.resolve("bittrex-binance").resolve("A-B-bittrex-binance_19700101010000001.jsons")).hasContent(expectedContent)
    }

    @Test
    fun shouldReadProfitsFromFile() {
        // given
        arbitrageProfitRepository.saveAll(currencyPairWithExchangePair, profitsToSave)
        // when
        val profitsRead = arbitrageProfitRepository.getProfits(currencyPairWithExchangePair)
        // then
        assertThat(profitsRead).isEqualTo(profitsToSave)
    }

    @Test
    fun shouldRemoveTooOldProfits() {
        // given
        val timeMillis = ArrayDeque<Long>(listOf(1010L, 1015L, 1017L))
        val repository = FileOrderBookArbitrageProfitRepository(profitsFolder.absolutePath, 12L, objectMapper) { timeMillis.pollFirst() }
        repository.saveAll(currencyPairWithExchangePair, profitsToSave + sampleProfit3)
        // when
        repository.removeTooOldProfits(currencyPairWithExchangePair)
        // then
        val profitsAfterRemoval = repository.getProfits(currencyPairWithExchangePair)
        // then
        assertThat(profitsAfterRemoval).hasSize(2)
        assertThat(profitsAfterRemoval[0].calculatedAtMillis).isEqualTo(sampleProfit2.calculatedAtMillis)
        assertThat(profitsAfterRemoval[1].calculatedAtMillis).isEqualTo(sampleProfit3.calculatedAtMillis)
    }


    @Test
    fun shouldRemoveAllButLatestTickerPairFile() {
        // given
        val timeMillis = ArrayDeque<Long>(listOf(1L, 2L, 3L))
        val repository = FileOrderBookArbitrageProfitRepository(profitsFolder.absolutePath, 100L, objectMapper) { timeMillis.poll() }
        val otherCurrencyPairInTheSameDirectory = currencyPairWithExchangePair.copy(currencyPair = CurrencyPair.of("B/C"))
        repository.saveAll(currencyPairWithExchangePair, profitsToSave)
        repository.saveAll(currencyPairWithExchangePair, profitsToSave)
        repository.saveAll(otherCurrencyPairInTheSameDirectory, profitsToSave)

        // when
        repository.removeAllButLatestTickerPairFile(currencyPairWithExchangePair)
        // then
        assertThat(profitsFolder.resolve("bittrex-binance").resolve("A-B-bittrex-binance_19700101010000001.jsons")).doesNotExist()
        assertThat(profitsFolder.resolve("bittrex-binance").resolve("A-B-bittrex-binance_19700101010000002.jsons")).exists()
        // make sure only invoked currency pair file was removed, others untouched
        assertThat(profitsFolder.resolve("bittrex-binance").resolve("B-C-bittrex-binance_19700101010000003.jsons")).exists()
    }

    @Test
    fun shouldGetAllCurrencyPairsWithExchangePairs() {
        // given
        val tickerPairsToSaveDoNotMatter = profitsToSave
        arbitrageProfitRepository.saveAll(
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("A/B"), exchangePair = ExchangePair(BITTREX, BINANCE)),
                tickerPairsToSaveDoNotMatter
        )
        arbitrageProfitRepository.saveAll(
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("C/D"), exchangePair = ExchangePair(BITTREX, BINANCE)),
                tickerPairsToSaveDoNotMatter
        )
        arbitrageProfitRepository.saveAll(
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("C/D"), exchangePair = ExchangePair(KUCOIN, BINANCE)),
                tickerPairsToSaveDoNotMatter
        )
        arbitrageProfitRepository.saveAll( // save second time the same to have 2 files in folder
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("A/B"), exchangePair = ExchangePair(BITTREX, BINANCE)),
                tickerPairsToSaveDoNotMatter
        )
        // when
        val allCurrencyPairsWithExchangePairs = arbitrageProfitRepository.getAllCurrencyPairsWithExchangePairs()
        // then
        assertThat(allCurrencyPairsWithExchangePairs).containsOnly(
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("A/B"), exchangePair = ExchangePair(BITTREX, BINANCE)),
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("C/D"), exchangePair = ExchangePair(BITTREX, BINANCE)),
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("C/D"), exchangePair = ExchangePair(KUCOIN, BINANCE))
        )
    }


    @Test
    fun shouldGetAllCurrencyPairsWithExchangePairsWhenNothingSavedYet() {
        // when
        val allCurrencyPairsWithExchangePairs = arbitrageProfitRepository.getAllCurrencyPairsWithExchangePairs()
        // then
        assertThat(allCurrencyPairsWithExchangePairs).isEmpty()
    }

    @Test
    fun shouldGetAllCurrencyPairsWithExchangePairsWhenExchangeDirectoryDoesNotExistYet() {
        // given
        val tickerPairRepository = FileOrderBookArbitrageProfitRepository("/non-existing-path", 100L, objectMapper)
        // when
        val allCurrencyPairsWithExchangePairs = tickerPairRepository.getAllCurrencyPairsWithExchangePairs()
        // then
        assertThat(allCurrencyPairsWithExchangePairs).isEmpty()
    }
}