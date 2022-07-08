package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.ticker.CurrencyPairWithExchangePair

interface OrderBookArbitrageProfitRepository {

    fun getAllCurrencyPairsWithExchangePairs(): List<CurrencyPairWithExchangePair>

    fun getProfits(currencyPairWithExchangePair: CurrencyPairWithExchangePair): List<TwoLegOrderBookArbitrageProfit>
}
