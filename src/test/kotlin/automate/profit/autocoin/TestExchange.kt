package automate.profit.autocoin

import com.autocoin.exchangegateway.spi.exchange.Exchange

object TestExchange {
    val exchangeA = object : Exchange {
        override val exchangeName = "exchangeA"
    }
    val exchangeB = object : Exchange {
        override val exchangeName = "exchangeB"
    }
    val exchangeC = object : Exchange {
        override val exchangeName = "exchangeC"
    }
    val exchangeD = object : Exchange {
        override val exchangeName = "exchangeD"
    }
    val exchangeE = object : Exchange {
        override val exchangeName = "exchangeE"
    }
}
