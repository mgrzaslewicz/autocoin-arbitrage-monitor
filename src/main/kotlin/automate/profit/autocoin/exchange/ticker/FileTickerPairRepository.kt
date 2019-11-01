package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.currency.CurrencyPair
import java.io.File
import java.math.RoundingMode
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

class FileTickerPairRepository(
        private val tickerRepositoryPath: String,
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

    private fun getCurrencyPairFileNamePrefix(currencyPair: CurrencyPair, exchangePair: ExchangePair): String {
        return "${currencyPair.base}-${currencyPair.counter}-${exchangePair.firstExchange.exchangeName}-${exchangePair.secondExchange.exchangeName}"
    }

    fun saveAll(currencyPair: CurrencyPair, exchangePair: ExchangePair, tickerPairsToSave: List<TickerPair>) {
        val exchangeDirectory = getOrCreateExchangePairDirectory(exchangePair)
        val currentDateTime = getCurrentDateTimeAsString()
        val tickersFileName = "${getCurrencyPairFileNamePrefix(currencyPair, exchangePair)}_$currentDateTime.csv"
        val tickersFile = exchangeDirectory.resolve(tickersFileName)
        val tickerPairsCsvLines = StringBuffer()
        tickerPairsToSave.forEach {
            tickerPairsCsvLines.append(it.toCsvLine())
            tickerPairsCsvLines.appendln()
        }
        synchronized(this) {
            tickersFile.writeText(tickerPairsCsvLines.toString())
        }
    }

    private fun getOrCreateExchangePairDirectory(exchangePair: ExchangePair): File {
        val directoryName = "${exchangePair.firstExchange.exchangeName}-${exchangePair.secondExchange.exchangeName}"
        val result = tickerRepositoryDirectory.resolve(directoryName)
        if (!result.exists()) {
            check(result.mkdirs()) { "Could not create directory $directoryName" }
        }
        return result
    }

    fun getTickerPairs(currencyPair: CurrencyPair, exchangePair: ExchangePair): List<TickerPair> {
        synchronized(this) {
            val tickerPairsFile = getLatestTickerPairsFile(currencyPair, exchangePair)
            return tickerPairsFile?.readLines()?.map { it.toTickerPair(currencyPair) } ?: emptyList()
        }
    }

    private fun getLatestTickerPairsFile(currencyPair: CurrencyPair, exchangePair: ExchangePair): File? {
        val directory = getOrCreateExchangePairDirectory(exchangePair)
        val latestFileName = directory
                .list()
                .filter { it.contains(".csv") && it.contains(getCurrencyPairFileNamePrefix(currencyPair, exchangePair)) }
                .maxBy { getNumberFromName(it) }
        return if (latestFileName != null) {
            directory.resolve(latestFileName)
        } else null
    }


    /**
     * A-B-bittrex-binance_12345.csv -> 12345
     */
    private fun getNumberFromName(fileName: String): Long {
        val exchangeNameAndDateTime = fileName.split("_", ".csv")
        return exchangeNameAndDateTime[1].toLong()
    }

    fun removeAllButLatestTickerPairFile(currencyPair: CurrencyPair, exchangePair: ExchangePair) {
        synchronized(this) {
            val latestTickerPairFile = getLatestTickerPairsFile(currencyPair, exchangePair)
            getOrCreateExchangePairDirectory(exchangePair)
                    .list()
                    .filterNot { it == latestTickerPairFile?.name }
                    .forEach { latestTickerPairFile?.resolveSibling(it)?.delete() }
        }
    }

}