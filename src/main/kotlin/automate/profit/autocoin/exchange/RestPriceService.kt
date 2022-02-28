package automate.profit.autocoin.exchange

import automate.profit.autocoin.logger.PeriodicalLogger
import automate.profit.autocoin.metrics.MetricsService
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request
import java.math.BigDecimal
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.ConcurrentHashMap


data class CurrencyPriceDto(
    val price: Double,
    val baseCurrency: String,
    val counterCurrency: String
)

interface PriceService {
    fun getUsdPrice(currencyCode: String): BigDecimal
    fun getUsdValue(currencyCode: String, amount: BigDecimal): BigDecimal
}

class RestPriceService(
    private val priceApiUrl: String,
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    private val metricsService: MetricsService,
    private val currentTimeMillisFunction: () -> Long = System::currentTimeMillis,
) : PriceService {

    private companion object {
        private val logger = PeriodicalLogger(wrapped = KotlinLogging.logger {}).scheduleLogFlush()
    }

    override fun getUsdPrice(currencyCode: String): BigDecimal {
        if (currencyCode == "USD") {
            return BigDecimal.ONE
        }
        return fetchUsdPrice(currencyCode)
    }

    override fun getUsdValue(currencyCode: String, amount: BigDecimal): BigDecimal {
        val price = getUsdPrice(currencyCode)
        return amount.multiply(price)
    }

    private fun fetchUsdPrice(currencyCode: String): BigDecimal {
        logger.frequentInfo { "Fetching price for $currencyCode" }
        val millisBefore = currentTimeMillisFunction()
        val request = Request.Builder()
            .url("$priceApiUrl/prices/USD?currencyCodes=${currencyCode}")
            .get()
            .build()
        val priceResponse = httpClient.newCall(request).execute()
        priceResponse.use {
            metricsService.recordFetchPriceTime(currentTimeMillisFunction() - millisBefore, "currencyCode=$currencyCode,statusCode=${priceResponse.code}")
            check(priceResponse.isSuccessful) { "Could not get price for $currencyCode/USD, code=${priceResponse.code}" }
            val priceDto = objectMapper.readValue(priceResponse.body?.string(), Array<CurrencyPriceDto>::class.java)
            check(priceDto.size == 1) { "No required price in response for $currencyCode" }
            return priceDto.first().price.toBigDecimal()
        }
    }

}
