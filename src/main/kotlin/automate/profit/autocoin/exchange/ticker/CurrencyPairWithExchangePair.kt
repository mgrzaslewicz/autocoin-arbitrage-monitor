package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.app.ExchangePair
import automate.profit.autocoin.exchange.currency.CurrencyPair

data class CurrencyPairWithExchangePair(
    val currencyPair: CurrencyPair,
    /**
     * Order of first/second exchange does mean buy/sell. exchangePair is set before opportonity calculation
     */
    val exchangePair: ExchangePair
)
