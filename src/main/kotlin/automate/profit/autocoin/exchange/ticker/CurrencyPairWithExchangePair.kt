package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.config.ExchangePair
import automate.profit.autocoin.exchange.currency.CurrencyPair

data class CurrencyPairWithExchangePair(
        val currencyPair: CurrencyPair,
        val exchangePair: ExchangePair
)