package automate.profit.autocoin.exchange.metadata

import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request

class RestExchangeMetadataService(
    private val httpClient: OkHttpClient,
    private val exchangeMetadataApiBaseurl: String,
    private val objectMapper: ObjectMapper
) : ExchangeMetadataService {

    private val logger = KotlinLogging.logger {}

    override fun getAllExchangesMetadata(): List<ExchangeMetadata> {
        val exchangesMetadataEndpointUrl = "$exchangeMetadataApiBaseurl/metadata/exchanges"
        logger.debug { "Requesting all exchanges metadata @$exchangesMetadataEndpointUrl" }
        val metadataResponse = httpClient.newCall(
            Request.Builder()
                .get()
                .url(exchangesMetadataEndpointUrl)
                .build()
        ).execute()
        metadataResponse.use {
            check(metadataResponse.isSuccessful) { "Could not get all exchanges metadata response, code=${metadataResponse.code}" }
            return objectMapper.readValue(metadataResponse.body?.string(), Array<ExchangeMetadataDto>::class.java)
                .map { it.toExchangeMetadata() }
        }
    }

    override fun getMetadata(exchangeName: String): ExchangeMetadata {
        val exchangeMetadataEndpointUrl = "$exchangeMetadataApiBaseurl/metadata/$exchangeName"
        logger.debug { "[$exchangeName] Requesting exchange metadata @$exchangeMetadataEndpointUrl" }
        val metadataResponse = httpClient.newCall(
            Request.Builder()
                .get()
                .url(exchangeMetadataEndpointUrl)
                .build()
        ).execute()
        metadataResponse.use {
            check(metadataResponse.isSuccessful) { "[$exchangeName] Could not get exchange metadata response, code=${metadataResponse.code}" }
            return objectMapper.readValue(metadataResponse.body?.string(), ExchangeMetadataDto::class.java)
                .toExchangeMetadata()
        }

    }

    override fun getCurrencyPairMetadata(exchangeName: String, currencyPair: CurrencyPair): CurrencyPairMetadata {
        logger.info { "[$exchangeName-$currencyPair] Requesting currency pair metadata" }
        return getMetadata(exchangeName).currencyPairMetadata.getValue(currencyPair)
    }
}
