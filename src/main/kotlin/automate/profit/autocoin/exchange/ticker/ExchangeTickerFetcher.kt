package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.exchange.SupportedExchange
import automate.profit.autocoin.exchange.currency.CurrencyPair
import automate.profit.autocoin.ticker.TickerDto
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.time.Instant

class ExchangeTickerFetcher(
        private val supportedExchange: SupportedExchange,
        private val tickerApiUrl: String,
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        private val currentTimeMillis: () -> Long = System::currentTimeMillis
) : UserExchangeTickerService {
    companion object : KLogging()

    override fun getTicker(currencyPair: CurrencyPair): Ticker {
        logger.debug { "Requesting $supportedExchange-$currencyPair" }
        val request = Request.Builder()
                .url("$tickerApiUrl/ticker/${supportedExchange.exchangeName}/${currencyPair.base}/${currencyPair.counter}")
                .get()
                .build()
        val tickerDtoResponse = httpClient.newCall(request).execute()
        tickerDtoResponse.use {
            check(tickerDtoResponse.code == 200) { "Could not get ticker $supportedExchange-$currencyPair" }

            val tickerDto = objectMapper.readValue(tickerDtoResponse.body?.string(), TickerDto::class.java)
            tickerDtoResponse.body?.close()
            val ticker = tickerDto.toTicker()
            return if (ticker.hasTimestamp()) {
                ticker
            } else {
                ticker.copy(timestamp = Instant.ofEpochMilli(currentTimeMillis()))
            }
        }
    }
}