package automate.profit.autocoin.exchange

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.FileTickerPairRepository
import automate.profit.autocoin.exchange.ticker.Ticker
import automate.profit.autocoin.exchange.ticker.TickerPair
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
    private val exchangePair = ExchangePair(SupportedExchange.BITTREX, SupportedExchange.BINANCE)
    private val tickerPairsToSave = listOf(
            TickerPair(
                    Ticker(currencyPair = currencyPair, ask = BigDecimal("1.001"), bid = BigDecimal("1.0011"), timestamp = Instant.ofEpochMilli(1000)),
                    Ticker(currencyPair = currencyPair, ask = BigDecimal("1.002"), bid = BigDecimal("1.0021"), timestamp = Instant.ofEpochMilli(1000))
            ),
            TickerPair(
                    Ticker(currencyPair = currencyPair, ask = BigDecimal("1.001"), bid = BigDecimal("1.0011"), timestamp = Instant.ofEpochMilli(1005)),
                    Ticker(currencyPair = currencyPair, ask = BigDecimal("1.002"), bid = BigDecimal("1.0021"), timestamp = Instant.ofEpochMilli(1005))
            )
    )
    private lateinit var tickerPairRepository: FileTickerPairRepository
    private lateinit var tickersFolder: File

    @BeforeEach
    fun setup() {
        tempFolder.create()
        tickersFolder = tempFolder.newFolder()
        tickerPairRepository = FileTickerPairRepository(tickersFolder.absolutePath) { 1L }
    }

    @AfterEach
    fun cleanup() {
        tempFolder.delete()
    }

    @Test
    fun shouldCreateDirectoryForTickerPairs() {
        // when
        tickerPairRepository.saveAll(currencyPair, exchangePair, tickerPairsToSave)
        // then
        assertThat(tickersFolder).isDirectoryContaining { it.name == "bittrex-binance" }
    }

    @Test
    fun shouldCreateTickerPairsFile() {
        // when
        tickerPairRepository.saveAll(currencyPair, exchangePair, tickerPairsToSave)
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
        tickerPairRepository.saveAll(currencyPair, exchangePair, tickerPairsToSave)
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
        tickerPairRepository.saveAll(currencyPair, exchangePair, tickerPairsToSave)
        // when
        val tickerPairsRead = tickerPairRepository.getTickerPairs(currencyPair, exchangePair)
        // then
        assertThat(tickerPairsRead).isEqualTo(tickerPairsToSave.map {
            it.copy(
                    first = it.first.withScaledPrice(8),
                    second = it.second.withScaledPrice(8)
            )
        })
    }

    @Test
    fun shouldRemoveAllButLatestTickerPairFile() {
        // given
        val timeMillis = ArrayDeque<Long>(listOf(1L, 2L))
        tickerPairRepository = FileTickerPairRepository(tickersFolder.absolutePath) { timeMillis.poll() }
        tickerPairRepository.saveAll(currencyPair, exchangePair, tickerPairsToSave)
        tickerPairRepository.saveAll(currencyPair, exchangePair, tickerPairsToSave)
        // when
        tickerPairRepository.removeAllButLatestTickerPairFile(currencyPair, exchangePair)
        // then
        assertThat(tickersFolder.resolve("bittrex-binance").resolve("A-B-bittrex-binance_19700101010000001.csv")).doesNotExist()
        assertThat(tickersFolder.resolve("bittrex-binance").resolve("A-B-bittrex-binance_19700101010000002.csv")).exists()
    }

}