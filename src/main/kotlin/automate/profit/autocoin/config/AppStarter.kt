package automate.profit.autocoin.config

import automate.profit.autocoin.exchange.metadata.CommonExchangeCurrencyPairsService
import automate.profit.autocoin.exchange.ticker.TickerListenerRegistrars
import automate.profit.autocoin.exchange.ticker.TickerListenersProvider
import automate.profit.autocoin.scheduled.ArbitrageProfitStatisticsCalculateScheduler
import automate.profit.autocoin.scheduled.TickerFetchScheduler
import automate.profit.autocoin.scheduled.TickerPairsSaveScheduler
import io.undertow.Undertow
import mu.KLogging

class AppStarter(
        private val commonExchangeCurrencyPairsService: CommonExchangeCurrencyPairsService,
        private val tickerListenersProvider: TickerListenersProvider,
        private val tickerFetchScheduler: TickerFetchScheduler,
        private val tickerListenerRegistrars: TickerListenerRegistrars,
        private val tickerPairsSaveScheduler: TickerPairsSaveScheduler,
        private val profitStatisticsCalculateScheduler: ArbitrageProfitStatisticsCalculateScheduler,
        private val server: Undertow
) {
    companion object : KLogging()

    fun start() {
        logger.info { "Fetching currency pairs from exchanges" }
        val commonCurrencyPairs = commonExchangeCurrencyPairsService.getCommonCurrencyPairs()
        val tickerListeners = tickerListenersProvider.createTickerListenersFrom(commonCurrencyPairs)
        logger.info { "Registering ${tickerListeners.size} ticker listeners" }
        tickerListeners.forEach { tickerListenerRegistrars.registerTickerListener(it) }
        logger.info { "Scheduling fetching tickers" }
        tickerFetchScheduler.scheduleFetchingTickers()
        logger.info { "Scheduling saving tickers to files" }
        tickerPairsSaveScheduler.scheduleSavingTickerPairs()
        logger.info { "Scheduling calculating arbitrage profit statistics" }
        profitStatisticsCalculateScheduler.scheduleCacheRefresh()
        logger.info { "Starting server" }
        server.start()
    }
}