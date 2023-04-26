package automate.profit.autocoin.app

import automate.profit.autocoin.app.config.ExchangePair
import automate.profit.autocoin.exchange.currency.CurrencyPair
import mu.KLogging
import java.net.SocketAddress

data class StartedApp(
    val serverAddress: SocketAddress
)

class AppStarter(private val appContext: AppContext) {
    private companion object : KLogging()

    fun start(): StartedApp {
        logger.info { "Fetching currency pairs from exchanges" }
        with(appContext) {
            val commonCurrencyPairs = commonExchangeCurrencyPairsService.calculateCommonCurrencyPairs()
            logCommonCurrencyPairsBetweenExchangePairs(commonCurrencyPairs.exchangePairsToCurrencyPairs)

            val twoLegArbitrageMonitors = twoLegArbitrageMonitorProvider.getTwoLegArbitrageOpportunitiesMonitors(commonCurrencyPairs.currencyPairsToExchangePairs)

            orderBookListeners.prepareOrderBookListeners(twoLegArbitrageMonitors)
            healthService.addOrderBookListenersTo(orderBookListeners)
            tickerListeners.prepareTickerListeners(twoLegArbitrageMonitors)
            healthService.addTickerListenersTo(tickerListeners)

            orderBookSseStreamService.startListeningOrderBookStream(commonCurrencyPairs)
            tickerSseStreamService.startListeningTickerStream(commonCurrencyPairs)

            logger.info { "Scheduling jobs" }
            orderBookSseStreamService.scheduleReconnectOnFailure(commonCurrencyPairs)
            tickerSseStreamService.scheduleReconnectOnFailure(commonCurrencyPairs)
            healthMetricsScheduler.scheduleSendingMetrics()
            twoLegOrderBookArbitrageProfitCacheScheduler.scheduleRemovingTooOldAndSendingMetrics()

            logger.info { "Starting server" }
            server.start()
            return StartedApp(
                serverAddress = server.listenerInfo[0].address
            )
        }
    }

    private fun logCommonCurrencyPairsBetweenExchangePairs(exchangePairToCurrencyPairs: Map<ExchangePair, Set<CurrencyPair>>) {
        appContext.appConfig.exchangesToMonitorTwoLegArbitrageOpportunities.forEachIndexed { index, supportedExchange ->
            for (i in index + 1 until appContext.appConfig.exchangesToMonitorTwoLegArbitrageOpportunities.size) {
                val exchangePair = ExchangePair(
                    firstExchange = supportedExchange,
                    secondExchange = appContext.appConfig.exchangesToMonitorTwoLegArbitrageOpportunities[i]
                )
                val currencyPairs = exchangePairToCurrencyPairs[exchangePair]
                logger.info { "Number common of currency pairs for $exchangePair = ${currencyPairs?.size ?: 0}" }
            }
        }
    }
}
