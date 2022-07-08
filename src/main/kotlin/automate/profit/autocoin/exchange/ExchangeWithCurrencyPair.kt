package automate.profit.autocoin.exchange

import automate.profit.autocoin.exchange.currency.CurrencyPair

data class ExchangeWithCurrencyPair(
    val exchange: SupportedExchange,
    val currencyPair: CurrencyPair
)
