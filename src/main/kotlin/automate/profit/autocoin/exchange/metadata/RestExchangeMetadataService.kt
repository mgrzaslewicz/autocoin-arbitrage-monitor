package automate.profit.autocoin.exchange.metadata

import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.fasterxml.jackson.databind.ObjectMapper
import mu.KotlinLogging
import okhttp3.OkHttpClient
import okhttp3.Request


private val logger = KotlinLogging.logger {}

class RestExchangeMetadataService(
        private val httpClient: OkHttpClient,
        private val exchangeMetadataServiceHostWithPort: String,
        private val objectMapper: ObjectMapper
) : ExchangeMetadataService {

    override fun getMetadata(exchangeName: String): ExchangeMetadata {
        val exchangeMetadataApiUrl = "$exchangeMetadataServiceHostWithPort/metadata/$exchangeName"
        logger.debug { "Requesting metadata for exchange $exchangeName @$exchangeMetadataApiUrl" }
        val metadataResponse = httpClient.newCall(Request.Builder()
                .get()
                .url(exchangeMetadataApiUrl)
                .build()
        ).execute()
        metadataResponse.use {
            check(metadataResponse.code == 200) { "Could not get exchange metadata response, code=${metadataResponse.code}" }
            return objectMapper.readValue(metadataResponse.body?.string(), ExchangeMetadataDto::class.java)
                    .toExchangeMetadata()
        }

    }

    override fun getMetadata(exchangeName: String, currencyPair: CurrencyPair): CurrencyPairMetadata {
        logger.info { "Requesting metadata for exchange $exchangeName and currency pair $currencyPair" }
        return getMetadata(exchangeName).currencyPairMetadata.getValue(currencyPair)
    }
}
