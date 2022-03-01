package automate.profit.autocoin.api

import automate.profit.autocoin.metrics.MetricsService
import io.undertow.Undertow
import io.undertow.UndertowOptions
import io.undertow.server.HttpHandler
import io.undertow.server.RoutingHandler
import io.undertow.util.HttpString.tryFromString

class ServerBuilder(
    val appServerPort: Int,
    private val apiControllers: List<ApiController>,
    private val metricsService: MetricsService
) {
    fun build(): Undertow {
        val routingHandler = RoutingHandler()
        apiControllers.forEach {
            it.apiHandlers().forEach { handler ->
                routingHandler.add(handler.method, handler.urlTemplate, handler.httpHandler)
            }
        }
        return Undertow.builder()
            .addHttpListener(appServerPort, "0.0.0.0")
            .setHandler(
                routingHandler
                    .wrapWithOptionsHandler()
                    .wrapWithCorsHeadersHandler()
                    .wrapWithRequestMetricsHandler()
            )
            .setServerOption(UndertowOptions.RECORD_REQUEST_START_TIME, true)
            .build()
    }

    private fun HttpHandler.wrapWithCorsHeadersHandler(): HttpHandler {
        return HttpHandler {
            with(it.responseHeaders) {
                put(tryFromString("Access-Control-Allow-Origin"), "*")
                put(tryFromString("Access-Control-Allow-Credentials"), "true")
                put(tryFromString("Access-Control-Allow-Headers"), "Origin, X-Requested-With, Content-Type, Accept, Authorization, Cache-Control")
                put(tryFromString("Access-Control-Allow-Methods"), "GET, HEAD, POST, PUT, DELETE, OPTIONS")
                put(tryFromString("Access-Control-Max-Age"), "3600")
            }
            this.handleRequest(it)
        }
    }

    private fun HttpHandler.wrapWithOptionsHandler(): HttpHandler {
        return HttpHandler {
            if (it.requestMethod.toString().uppercase() == "OPTIONS") {
                it.statusCode = 204 // no content
                it.responseSender.send("")
            } else {
                this.handleRequest(it)
            }
        }
    }

    private fun HttpHandler.wrapWithRequestMetricsHandler(): HttpHandler {
        return HttpHandler {
            this.handleRequest(it)
            metricsService.recordRequest(
                method = it.requestMethod.toString(),
                requestURI = it.requestPath,
                status = it.statusCode,
                executionTime = System.currentTimeMillis() - it.requestStartTime,
                // TODO add userName providing proper http handler chain, with current implementation it has null authenticatedAccount @see branch user-in-metrics
                additionalTags = emptyMap(),
            )
        }
    }

}
