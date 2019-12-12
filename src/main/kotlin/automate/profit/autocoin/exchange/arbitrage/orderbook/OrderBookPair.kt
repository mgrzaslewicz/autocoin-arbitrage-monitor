package automate.profit.autocoin.exchange.arbitrage.orderbook

import automate.profit.autocoin.exchange.orderbook.OrderBook

data class OrderBookPair(
        val first: OrderBook,
        val second: OrderBook
)
