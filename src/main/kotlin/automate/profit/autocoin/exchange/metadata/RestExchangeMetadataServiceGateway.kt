package automate.profit.autocoin.exchange.metadata

import com.autocoin.exchangegateway.dto.exchange.metadata.ExchangeMetadataDto
import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.ExchangeProvider
import com.autocoin.exchangegateway.spi.exchange.metadata.ExchangeMetadata
import com.autocoin.exchangegateway.spi.exchange.metadata.gateway.AuthorizedMetadataServiceGateway
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request

class RestExchangeMetadataServiceGateway(
    private val httpClient: OkHttpClient,
    private val exchangeMetadataApiBaseurl: String,
    private val objectMapper: ObjectMapper,
    private val exchangeProvider: ExchangeProvider,
) : AuthorizedMetadataServiceGateway {
    private val logger = KotlinLogging.logger {}


    override fun getAllExchangesMetadata(): Map<Exchange, ExchangeMetadata> {
        val exchangesMetadataEndpointUrl = "$exchangeMetadataApiBaseurl/v2/metadata/exchanges"
        logger.debug { "Requesting all exchanges metadata @$exchangesMetadataEndpointUrl" }
        val metadataResponse = httpClient.newCall(
            Request.Builder()
                .get()
                .url(exchangesMetadataEndpointUrl)
                .build()
        ).execute()
        metadataResponse.use {
            check(metadataResponse.isSuccessful) { "Could not get all exchanges metadata response, code=${metadataResponse.code}, body=${it.body?.string()}, headers=${it.headers}" }
            return@getAllExchangesMetadata objectMapper.readValue(
                metadataResponse.body?.string(),
                Array<ExchangeMetadataDto>::class.java
            )
                .map { it.toExchangeMetadata(exchangeProvider) }
                .associateBy { it.exchange }
        }
    }

    override fun getMetadata(exchange: Exchange): ExchangeMetadata {
        val exchangeMetadataEndpointUrl = "$exchangeMetadataApiBaseurl/v2/metadata/${exchange.exchangeName}"
        logger.debug { "[${exchange.exchangeName}] Requesting exchange metadata @$exchangeMetadataEndpointUrl" }
        val metadataResponse = httpClient.newCall(
            Request.Builder()
                .get()
                .url(exchangeMetadataEndpointUrl)
                .build()
        ).execute()
        metadataResponse.use {
            check(metadataResponse.isSuccessful) { "[${exchange.exchangeName}] Could not get exchange metadata response, code=${metadataResponse.code}" }
            return objectMapper
                .readValue(metadataResponse.body?.string(), ExchangeMetadataDto::class.java)
                .toExchangeMetadata(exchangeProvider)
        }
    }

}
