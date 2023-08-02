package automate.profit.autocoin.exchange.ticker

import automate.profit.autocoin.app.config.ExchangePair
import com.autocoin.exchangegateway.spi.exchange.currency.CurrencyPair

data class CurrencyPairWithExchangePair(
    val currencyPair: CurrencyPair,
    /**
     * Order of first/second exchange does mean buy/sell. exchangePair is set before opportonity calculation
     */
    val exchangePair: ExchangePair
)
