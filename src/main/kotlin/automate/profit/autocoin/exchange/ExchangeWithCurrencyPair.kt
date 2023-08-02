package automate.profit.autocoin.exchange

import com.autocoin.exchangegateway.spi.exchange.Exchange
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair


data class ExchangeWithCurrencyPair(
    val exchange: Exchange,
    val currencyPair: CurrencyPair
)
