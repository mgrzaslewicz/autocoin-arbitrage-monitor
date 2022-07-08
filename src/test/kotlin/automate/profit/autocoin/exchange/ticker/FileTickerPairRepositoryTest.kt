package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange.*
import automate.profit.autocoin.exchange.currency.CurrencyPair
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.math.BigDecimal
import java.time.Instant
import java.util.*


class FileTickerPairRepositoryTest {
    private val tempFolder = TemporaryFolder()
    private val currencyPair = CurrencyPair.of("A/B")
    private val exchangePair = ExchangePair(BITTREX, BINANCE)
    private val currencyPairWithExchangePair = CurrencyPairWithExchangePair(currencyPair, exchangePair)
    private val tickerPair1 = TickerPair(
            Ticker(currencyPair = currencyPair, ask = BigDecimal("1.001"), bid = BigDecimal("1.0011"), timestamp = Instant.ofEpochMilli(1000)),
            Ticker(currencyPair = currencyPair, ask = BigDecimal("1.002"), bid = BigDecimal("1.0021"), timestamp = Instant.ofEpochMilli(1000))
    )
    private val tickerPair2 = TickerPair(
            Ticker(currencyPair = currencyPair, ask = BigDecimal("1.001"), bid = BigDecimal("1.0011"), timestamp = Instant.ofEpochMilli(1005)),
            Ticker(currencyPair = currencyPair, ask = BigDecimal("1.002"), bid = BigDecimal("1.0021"), timestamp = Instant.ofEpochMilli(1005))
    )
    private val tickerPairsToSave = listOf(tickerPair1, tickerPair2)
    private lateinit var tickerPairRepository: FileTickerPairRepository
    private lateinit var tickersFolder: File

    @BeforeEach
    fun setup() {
        tempFolder.create()
        tickersFolder = tempFolder.newFolder()
        tickerPairRepository = FileTickerPairRepository(tickersFolder.absolutePath, 100L) { 1L }
    }

    @AfterEach
    fun cleanup() {
        tempFolder.delete()
    }

    @Test
    fun shouldCreateDirectoryForTickerPairs() {
        // when
        tickerPairRepository.saveAll(currencyPairWithExchangePair, tickerPairsToSave)
        // then
        assertThat(tickersFolder).isDirectoryContaining { it.name == "bittrex-binance" }
    }

    @Test
    fun shouldCreateTickerPairsFile() {
        // when
        tickerPairRepository.saveAll(currencyPairWithExchangePair, tickerPairsToSave)
        // then
        assertThat(tickersFolder.resolve("bittrex-binance")).isDirectoryContaining { it.name == "A-B-bittrex-binance_19700101010000001.csv" }
    }

    @Test
    fun shouldSaveTickerPairsToFile() {
        // given
        val expectedContent = """
1.00100000,1.00110000,1000,1.00200000,1.00210000,1000
1.00100000,1.00110000,1005,1.00200000,1.00210000,1005
        """.trimIndent()
        // when
        tickerPairRepository.saveAll(currencyPairWithExchangePair, tickerPairsToSave)
        // then
        assertThat(tickersFolder.resolve("bittrex-binance").resolve("A-B-bittrex-binance_19700101010000001.csv")).hasContent(expectedContent)
    }

    @Test
    fun shouldAddTickerPairsToFile() {
        // given
        val expectedContent = """
1.00100000,1.00110000,1000,1.00200000,1.00210000,1000
1.00100000,1.00110000,1005,1.00200000,1.00210000,1005
1.00100000,1.00110000,1000,1.00200000,1.00210000,1000
1.00100000,1.00110000,1005,1.00200000,1.00210000,1005
        """.trimIndent()
        // when
        tickerPairRepository.addAll(currencyPairWithExchangePair, tickerPairsToSave)
        tickerPairRepository.addAll(currencyPairWithExchangePair, tickerPairsToSave)
        // then
        assertThat(tickersFolder.resolve("bittrex-binance").resolve("A-B-bittrex-binance_19700101010000001.csv")).hasContent(expectedContent)
    }

    fun Ticker.withScaledPrice(scale: Int) = this.copy(
            ask = ask.setScale(scale),
            bid = bid.setScale(scale)
    )

    @Test
    fun shouldReadTickerPairsFromFile() {
        // given
        tickerPairRepository.saveAll(currencyPairWithExchangePair, tickerPairsToSave)
        // when
        val tickerPairsRead = tickerPairRepository.getTickerPairs(currencyPairWithExchangePair)
        // then
        assertThat(tickerPairsRead).isEqualTo(tickerPairsToSave.map {
            it.copy(
                    first = it.first.withScaledPrice(8),
                    second = it.second.withScaledPrice(8)
            )
        })
    }

    @Test
    fun shouldRemoveTooOldTickers() {
        // given
        val timeMillis = ArrayDeque<Long>(listOf(1010L, 1015L, 1017L))
        val tickerPairRepository = FileTickerPairRepository(tickersFolder.absolutePath, 12L) { timeMillis.pollFirst() }
        tickerPairRepository.saveAll(currencyPairWithExchangePair, tickerPairsToSave)
        // when
        tickerPairRepository.removeTooOldTickers(currencyPairWithExchangePair)
        // then
        val tickerPairsAfterRemoval = tickerPairRepository.getTickerPairs(currencyPairWithExchangePair)
        // then
        assertThat(tickerPairsAfterRemoval).hasSize(1)
        assertThat(tickerPairsAfterRemoval[0].first.timestamp).isEqualTo(tickerPair2.first.timestamp)
        assertThat(tickerPairsAfterRemoval[0].second.timestamp).isEqualTo(tickerPair2.second.timestamp)
    }


    @Test
    fun shouldRemoveAllButLatestTickerPairFile() {
        // given
        val timeMillis = ArrayDeque<Long>(listOf(1L, 2L, 3L))
        val tickerPairRepository = FileTickerPairRepository(tickersFolder.absolutePath, 100L) { timeMillis.poll() }
        val otherCurrencyPairInTheSameDirectory = currencyPairWithExchangePair.copy(currencyPair = CurrencyPair.of("B/C"))
        tickerPairRepository.saveAll(currencyPairWithExchangePair, tickerPairsToSave)
        tickerPairRepository.saveAll(currencyPairWithExchangePair, tickerPairsToSave)
        tickerPairRepository.saveAll(otherCurrencyPairInTheSameDirectory, tickerPairsToSave)

        // when
        tickerPairRepository.removeAllButLatestTickerPairFile(currencyPairWithExchangePair)
        // then
        assertThat(tickersFolder.resolve("bittrex-binance").resolve("A-B-bittrex-binance_19700101010000001.csv")).doesNotExist()
        assertThat(tickersFolder.resolve("bittrex-binance").resolve("A-B-bittrex-binance_19700101010000002.csv")).exists()
        // make sure only invoked currency pair file was removed, others untouched
        assertThat(tickersFolder.resolve("bittrex-binance").resolve("B-C-bittrex-binance_19700101010000003.csv")).exists()
    }

    @Test
    fun shouldGetAllCurrencyPairsWithExchangePairs() {
        // given
        val tickerPairsToSaveDoNotMatter = tickerPairsToSave
        tickerPairRepository.saveAll(
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("A/B"), exchangePair = ExchangePair(BITTREX, BINANCE)),
                tickerPairsToSaveDoNotMatter
        )
        tickerPairRepository.saveAll(
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("C/D"), exchangePair = ExchangePair(BITTREX, BINANCE)),
                tickerPairsToSaveDoNotMatter
        )
        tickerPairRepository.saveAll(
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("C/D"), exchangePair = ExchangePair(KUCOIN, BINANCE)),
                tickerPairsToSaveDoNotMatter
        )
        tickerPairRepository.saveAll( // save second time the same to have 2 files in folder
                CurrencyPairWithExchangePair(currencyPair = CurrencyPair.of("A/B"), exchangePair = ExchangePair(BITTREX, BINANCE)),
                tickerPairsToSaveDoNotMatter
        )
        // when
        val allCurrencyPairsWithExchangePairs = tickerPairRepository.getAllCurrencyPairsWithExchangePairs()
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
        val allCurrencyPairsWithExchangePairs = tickerPairRepository.getAllCurrencyPairsWithExchangePairs()
        // then
        assertThat(allCurrencyPairsWithExchangePairs).isEmpty()
    }

    @Test
    fun shouldGetAllCurrencyPairsWithExchangePairsWhenExchangeDirectoryDoesNotExistYet() {
        // given
        val tickerPairRepository = FileTickerPairRepository("/non-existing-path", 100L)
        // when
        val allCurrencyPairsWithExchangePairs = tickerPairRepository.getAllCurrencyPairsWithExchangePairs()
        // then
        assertThat(allCurrencyPairsWithExchangePairs).isEmpty()
    }

}