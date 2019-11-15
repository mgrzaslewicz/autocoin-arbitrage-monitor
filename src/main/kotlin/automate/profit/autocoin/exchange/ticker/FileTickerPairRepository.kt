package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import java.io.File
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Stores ticker pairs in exchange pairs folders, eg
 * | binance-bittrex
 * | | ETH-BTC-binance-bittrex_{timestampAsMilliseconds}.csv
 * | binance-kucoin
 * | | ETH-BTC-binance-kucoin_{timestampAsMilliseconds}.csv
 */
class FileTickerPairRepository(
        tickerRepositoryPath: String,
        private val ageOfOldestTickerPairToKeepMs: Long,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    private fun TickerPair.toCsvLine(): String {
        val firstAsk = first.ask.setScale(8, RoundingMode.HALF_DOWN)
        val firstBid = first.bid.setScale(8, RoundingMode.HALF_DOWN)
        val secondAsk = second.ask.setScale(8, RoundingMode.HALF_DOWN)
        val secondBid = second.bid.setScale(8, RoundingMode.HALF_DOWN)
        return "${firstAsk},${firstBid},${first.timestamp?.toEpochMilli()
                ?: ""},${secondAsk},${secondBid},${second.timestamp?.toEpochMilli() ?: ""}"
    }

    private fun String.toTickerPair(currencyPair: CurrencyPair): TickerPair {
        val values = this.split(",")
        val firstTimestampString = values[2]
        val firstTimestamp = if (firstTimestampString.isEmpty()) null else Instant.ofEpochMilli(firstTimestampString.toLong())
        val secondTimestampString = values[5]
        val secondTimestamp = if (secondTimestampString.isEmpty()) null else Instant.ofEpochMilli(secondTimestampString.toLong())
        val firstTicker = Ticker(currencyPair = currencyPair, ask = values[0].toBigDecimal(), bid = values[1].toBigDecimal(), timestamp = firstTimestamp)
        val secondTicker = Ticker(currencyPair = currencyPair, ask = values[3].toBigDecimal(), bid = values[4].toBigDecimal(), timestamp = secondTimestamp)
        return TickerPair(firstTicker, secondTicker)
    }

    private val tickerRepositoryDirectory = File(tickerRepositoryPath)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
    private fun getCurrentDateTimeAsString() = dateTimeFormatter.format(Instant.ofEpochMilli(currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime())

    private fun getCurrencyPairFileNamePrefix(currencyPairWithExchangePair: CurrencyPairWithExchangePair): String {
        val currencyPair = currencyPairWithExchangePair.currencyPair
        val exchangePair = currencyPairWithExchangePair.exchangePair
        return "${currencyPair.base}-${currencyPair.counter}-${exchangePair.firstExchange.exchangeName}-${exchangePair.secondExchange.exchangeName}"
    }

    private fun createNewTickersFile(currencyPairWithExchangePair: CurrencyPairWithExchangePair): File {
        val exchangeDirectory = getOrCreateExchangePairDirectory(currencyPairWithExchangePair.exchangePair)
        val currentDateTime = getCurrentDateTimeAsString()
        val tickersFileName = "${getCurrencyPairFileNamePrefix(currencyPairWithExchangePair)}_$currentDateTime.csv"
        return exchangeDirectory.resolve(tickersFileName)
    }

    private fun List<TickerPair>.writeTo(file: File) {
        val tickerPairsCsvLines = StringBuffer()
        this.forEach {
            tickerPairsCsvLines.append(it.toCsvLine())
            tickerPairsCsvLines.appendln()
        }
        synchronized(this) {
            file.writeText(tickerPairsCsvLines.toString())
        }
    }

    private fun List<TickerPair>.appendTo(file: File) {
        val tickerPairsCsvLines = StringBuffer()
        this.forEach {
            tickerPairsCsvLines.append(it.toCsvLine())
            tickerPairsCsvLines.appendln()
        }
        synchronized(this) {
            file.appendText(tickerPairsCsvLines.toString())
        }
    }

    fun addAll(currencyPairWithExchangePair: CurrencyPairWithExchangePair, tickerPairsToSave: List<TickerPair>) {
        val tickersFile = getLatestTickerPairsFile(currencyPairWithExchangePair)
                ?: createNewTickersFile(currencyPairWithExchangePair)
        tickerPairsToSave.appendTo(tickersFile)
    }

    fun saveAll(currencyPairWithExchangePair: CurrencyPairWithExchangePair, tickerPairsToSave: List<TickerPair>) {
        val tickersFile = createNewTickersFile(currencyPairWithExchangePair)
        tickerPairsToSave.writeTo(tickersFile)
    }

    private fun getOrCreateExchangePairDirectory(exchangePair: ExchangePair): File {
        val directoryName = "${exchangePair.firstExchange.exchangeName}-${exchangePair.secondExchange.exchangeName}"
        val result = tickerRepositoryDirectory.resolve(directoryName)
        if (!result.exists()) {
            check(result.mkdirs()) { "Could not create directory $directoryName" }
        }
        return result
    }

    fun getTickerPairs(currencyPairWithExchangePair: CurrencyPairWithExchangePair): List<TickerPair> {
        synchronized(this) {
            val tickerPairsFile = getLatestTickerPairsFile(currencyPairWithExchangePair)
            return tickerPairsFile?.readLines()?.map { it.toTickerPair(currencyPairWithExchangePair.currencyPair) }
                    ?: emptyList()
        }
    }

    private fun getLatestTickerPairsFile(currencyPairWithExchangePair: CurrencyPairWithExchangePair): File? {
        val directory = getOrCreateExchangePairDirectory(currencyPairWithExchangePair.exchangePair)
        synchronized(this) {
            val latestFileName = directory
                    .list()
                    .filter { it.contains(".csv") && it.contains(getCurrencyPairFileNamePrefix(currencyPairWithExchangePair)) }
                    .maxBy { getNumberFromName(it) }
            return if (latestFileName != null) {
                directory.resolve(latestFileName)
            } else null
        }
    }


    /**
     * A-B-bittrex-binance_12345.csv -> 12345
     */
    private fun getNumberFromName(fileName: String): Long {
        val exchangeNameAndDateTime = fileName.split("_", ".csv")
        return exchangeNameAndDateTime[1].toLong()
    }

    fun removeAllButLatestTickerPairFile(currencyPairWithExchangePair: CurrencyPairWithExchangePair) {
        synchronized(this) {
            val latestTickerPairFile = getLatestTickerPairsFile(currencyPairWithExchangePair)
            getOrCreateExchangePairDirectory(currencyPairWithExchangePair.exchangePair)
                    .list()
                    .filter { it != latestTickerPairFile?.name && getCurrencyPairFrom(it) == currencyPairWithExchangePair.currencyPair }
                    .forEach { latestTickerPairFile?.resolveSibling(it)?.delete() }
        }
    }

    fun getAllCurrencyPairsWithExchangePairs(): Set<CurrencyPairWithExchangePair> {
        val exchangePairDirectories = tickerRepositoryDirectory.listFiles()?.filter { it.isDirectory }
        return exchangePairDirectories?.flatMap { exchangePairDirectory ->
            val exchangePair = getExchangePairFrom(exchangePairDirectory)
            exchangePairDirectory.listFiles { pathname -> pathname.name.endsWith(".csv") }
                    .map { csvFile -> CurrencyPairWithExchangePair(getCurrencyPairFrom(csvFile), exchangePair) }
        }?.toSet() ?: emptySet()
    }

    /**
     * a-b -> ExchangePair(a, b)
     */
    private fun getExchangePairFrom(exchangePairDirectory: File): ExchangePair {
        val nameParts = exchangePairDirectory.name.split("-")
        return ExchangePair(SupportedExchange.fromExchangeName(nameParts[0]), SupportedExchange.fromExchangeName(nameParts[1]))
    }

    /**
     * A-B-bittrex-binance_12345.csv -> CurrencyPair(A, B)
     */
    private fun getCurrencyPairFrom(csvFile: File): CurrencyPair {
        return getCurrencyPairFrom(csvFile.name)
    }

    private fun getCurrencyPairFrom(csvFile: String): CurrencyPair {
        val nameParts = csvFile.split("-")
        return CurrencyPair(nameParts[0], nameParts[1])
    }

    private fun TickerPair?.isOlderThan(maxAgeMillis: Long, currentTimeMillis: Long): Boolean {
        if (this == null) {
            return false
        }
        val firstTickerTimeMillis = this.first.timestamp?.toEpochMilli() ?: currentTimeMillis
        val secondTickerTimeMillis = this.second.timestamp?.toEpochMilli() ?: currentTimeMillis
        return currentTimeMillis - firstTickerTimeMillis > maxAgeMillis
                || currentTimeMillis - secondTickerTimeMillis > maxAgeMillis
    }

    /**
     * if file has too old tickers, new file will be created as a copy with skipped too old tickers
     */
    fun removeTooOldTickers(currencyPairWithExchangePair: CurrencyPairWithExchangePair) {
        val latestFile = getLatestTickerPairsFile(currencyPairWithExchangePair)
        val firstTickerPairInFile = latestFile?.useLines { it.firstOrNull() }?.toTickerPair(currencyPairWithExchangePair.currencyPair)
        val currentTimeMillis = currentTimeMillis()
        if (firstTickerPairInFile.isOlderThan(ageOfOldestTickerPairToKeepMs, currentTimeMillis)) {
            writeTickersToNewFileAndSkipTooOldTickers(latestFile!!, currencyPairWithExchangePair, currentTimeMillis)
        }
    }

    private fun writeTickersToNewFileAndSkipTooOldTickers(fileToCopy: File, currencyPairWithExchangePair: CurrencyPairWithExchangePair, currentTimeMillis: Long) {
        val newFile = createNewTickersFile(currencyPairWithExchangePair)
        fileToCopy.useLines { lines ->
            var firstLineNotToSkipFound = false
            lines.forEach { line ->
                if (firstLineNotToSkipFound) { // don't parse, just write strings
                    newFile.appendText(line)
                } else {
                    val tickerPair = line.toTickerPair(currencyPairWithExchangePair.currencyPair)
                    if (!tickerPair.isOlderThan(ageOfOldestTickerPairToKeepMs, currentTimeMillis)) {
                        newFile.appendText(line)
                    } else {
                        firstLineNotToSkipFound = true
                    }
                }
            }

        }
    }

}