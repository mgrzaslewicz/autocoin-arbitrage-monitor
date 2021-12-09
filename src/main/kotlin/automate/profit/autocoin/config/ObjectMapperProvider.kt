package automate.profit.autocoin.config

import automate.profit.autocoin.exchange.currency.CurrencyPair
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.KeyDeserializer
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.module.SimpleModule
import com.fasterxml.jackson.module.kotlin.KotlinModule

class CurrencyPairDeserializer : KeyDeserializer() {
    override fun deserializeKey(key: String, ctxt: DeserializationContext): Any {
        return CurrencyPair.of(key)
    }
}

class ObjectMapperProvider {
    fun createObjectMapper(): ObjectMapper {
        return ObjectMapper()
                .registerModule(KotlinModule.Builder().build())
                .registerModule(SimpleModule().addKeyDeserializer(CurrencyPair::class.java, CurrencyPairDeserializer()))
    }
}