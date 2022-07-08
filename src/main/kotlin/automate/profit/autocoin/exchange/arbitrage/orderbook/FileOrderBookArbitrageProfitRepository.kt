package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair
import com.fasterxml.jackson.databind.ObjectMapper
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Stores calculated arbitrage profits in exchange pairs folders, eg
 * | binance-bittrex
 * | | ETH-BTC-binance-bittrex_{timestampAsMilliseconds}.jsons
 * | binance-kucoin
 * | | ETH-BTC-binance-kucoin_{timestampAsMilliseconds}.jsons
 * Each calculated profit is serialized to json stored as separate line.
 * This allows to deserialize file line by line and check timestamp
 */
class FileOrderBookArbitrageProfitRepository(
        tickerRepositoryPath: String,
        private val ageOfOldestProfitToKeepMs: Long,
        private val objectMapper: ObjectMapper,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis
) {

    private fun TwoLegOrderBookArbitrageProfit.toJsonLine(): String {
        return objectMapper.writeValueAsString(this)
    }

    private fun String.toTwoLegOrderBookArbitrageProfit(): TwoLegOrderBookArbitrageProfit {
        return objectMapper.readValue(this, TwoLegOrderBookArbitrageProfit::class.java)
    }

    private val tickerRepositoryDirectory = File(tickerRepositoryPath)
    private val dateTimeFormatter = DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS")
    private fun getCurrentDateTimeAsString() = dateTimeFormatter.format(Instant.ofEpochMilli(currentTimeMillis()).atZone(ZoneId.systemDefault()).toLocalDateTime())

    private fun getCurrencyPairFileNamePrefix(currencyPairWithExchangePair: CurrencyPairWithExchangePair): String {
        val currencyPair = currencyPairWithExchangePair.currencyPair
        val exchangePair = currencyPairWithExchangePair.exchangePair
        return "${currencyPair.base}-${currencyPair.counter}-${exchangePair.firstExchange.exchangeName}-${exchangePair.secondExchange.exchangeName}"
    }

    private fun createNewFile(currencyPairWithExchangePair: CurrencyPairWithExchangePair): File {
        val exchangeDirectory = getOrCreateExchangePairDirectory(currencyPairWithExchangePair.exchangePair)
        val currentDateTime = getCurrentDateTimeAsString()
        val tickersFileName = "${getCurrencyPairFileNamePrefix(currencyPairWithExchangePair)}_$currentDateTime.jsons"
        return exchangeDirectory.resolve(tickersFileName)
    }

    private fun List<TwoLegOrderBookArbitrageProfit>.writeTo(file: File) {
        val tickerPairsCsvLines = StringBuffer()
        this.forEach {
            tickerPairsCsvLines.append(it.toJsonLine())
            tickerPairsCsvLines.appendln()
        }
        synchronized(this) {
            file.writeText(tickerPairsCsvLines.toString())
        }
    }

    private fun List<TwoLegOrderBookArbitrageProfit>.appendTo(file: File) {
        val tickerPairsCsvLines = StringBuffer()
        this.forEach {
            tickerPairsCsvLines.append(it.toJsonLine())
            tickerPairsCsvLines.appendln()
        }
        synchronized(this) {
            file.appendText(tickerPairsCsvLines.toString())
        }
    }

    fun addAll(currencyPairWithExchangePair: CurrencyPairWithExchangePair, profitsToSave: List<TwoLegOrderBookArbitrageProfit>) {
        val file = getLatestFile(currencyPairWithExchangePair)
                ?: createNewFile(currencyPairWithExchangePair)
        profitsToSave.appendTo(file)
    }

    fun saveAll(currencyPairWithExchangePair: CurrencyPairWithExchangePair, profitsToSave: List<TwoLegOrderBookArbitrageProfit>) {
        val file = createNewFile(currencyPairWithExchangePair)
        profitsToSave.writeTo(file)
    }

    private fun getOrCreateExchangePairDirectory(exchangePair: ExchangePair): File {
        val directoryName = "${exchangePair.firstExchange.exchangeName}-${exchangePair.secondExchange.exchangeName}"
        val result = tickerRepositoryDirectory.resolve(directoryName)
        if (!result.exists()) {
            check(result.mkdirs()) { "Could not create directory $directoryName" }
        }
        return result
    }

    fun getProfits(currencyPairWithExchangePair: CurrencyPairWithExchangePair): List<TwoLegOrderBookArbitrageProfit> {
        synchronized(this) {
            val tickerPairsFile = getLatestFile(currencyPairWithExchangePair)
            return tickerPairsFile?.readLines()?.map { it.toTwoLegOrderBookArbitrageProfit() }
                    ?: emptyList()
        }
    }

    private fun getLatestFile(currencyPairWithExchangePair: CurrencyPairWithExchangePair): File? {
        val directory = getOrCreateExchangePairDirectory(currencyPairWithExchangePair.exchangePair)
        synchronized(this) {
            val latestFileName = directory
                    .list()
                    .filter { it.contains(".jsons") && it.contains(getCurrencyPairFileNamePrefix(currencyPairWithExchangePair)) }
                    .maxBy { getNumberFromName(it) }
            return if (latestFileName != null) {
                directory.resolve(latestFileName)
            } else null
        }
    }


    /**
     * A-B-bittrex-binance_12345.jsons -> 12345
     */
    private fun getNumberFromName(fileName: String): Long {
        val exchangeNameAndDateTime = fileName.split("_", ".jsons")
        return exchangeNameAndDateTime[1].toLong()
    }

    fun removeAllButLatestTickerPairFile(currencyPairWithExchangePair: CurrencyPairWithExchangePair) {
        synchronized(this) {
            val latestTickerPairFile = getLatestFile(currencyPairWithExchangePair)
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
            exchangePairDirectory.listFiles { pathname -> pathname.name.endsWith(".jsons") }
                    .map { jsonsFile -> CurrencyPairWithExchangePair(getCurrencyPairFrom(jsonsFile), exchangePair) }
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
     * A-B-bittrex-binance_12345.jsons -> CurrencyPair(A, B)
     */
    private fun getCurrencyPairFrom(jsonsFile: File): CurrencyPair {
        return getCurrencyPairFrom(jsonsFile.name)
    }

    private fun getCurrencyPairFrom(jsonsFile: String): CurrencyPair {
        val nameParts = jsonsFile.split("-")
        return CurrencyPair(nameParts[0], nameParts[1])
    }

    private fun TwoLegOrderBookArbitrageProfit?.isOlderThan(maxAgeMillis: Long, currentTimeMillis: Long): Boolean {
        if (this == null) {
            return false
        }
        return currentTimeMillis - this.calculatedAtMillis > maxAgeMillis
    }

    /**
     * if file has too old profit calculations, new file will be created as a copy with skipped too old ones
     */
    fun removeTooOldProfits(currencyPairWithExchangePair: CurrencyPairWithExchangePair) {
        val latestFile = getLatestFile(currencyPairWithExchangePair)
        val firstTickerPairInFile = latestFile?.useLines { it.firstOrNull() }?.toTwoLegOrderBookArbitrageProfit()
        val currentTimeMillis = currentTimeMillis()
        if (firstTickerPairInFile.isOlderThan(ageOfOldestProfitToKeepMs, currentTimeMillis)) {
            writeToNewFileAndSkipTooOldProfits(latestFile!!, currencyPairWithExchangePair, currentTimeMillis)
        }
    }

    private fun writeToNewFileAndSkipTooOldProfits(fileToCopy: File, currencyPairWithExchangePair: CurrencyPairWithExchangePair, currentTimeMillis: Long) {
        val newFile = createNewFile(currencyPairWithExchangePair)
        fileToCopy.useLines { lines ->
            var firstLineNotToSkipFound = false
            lines.forEach { line ->
                val lineToAppend = StringBuffer(line).appendln().toString()
                if (firstLineNotToSkipFound) { // don't parse, just write strings
                    newFile.appendText(lineToAppend)
                } else {
                    val tickerPair = line.toTwoLegOrderBookArbitrageProfit()
                    if (!tickerPair.isOlderThan(ageOfOldestProfitToKeepMs, currentTimeMillis)) {
                        newFile.appendText(lineToAppend)
                    } else {
                        firstLineNotToSkipFound = true
                    }
                }
            }

        }
    }

}
