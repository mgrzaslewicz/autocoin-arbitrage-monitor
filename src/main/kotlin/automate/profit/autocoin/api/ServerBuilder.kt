package automate.profit.autocoin.api

import io.undertow.Undertow
import io.undertow.server.HttpHandler
import io.undertow.server.RoutingHandler
import io.undertow.util.HttpString.tryFromString

class ServerBuilder(
        private val appServerPort: Int,
        private val apiControllers: List<ApiController>
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
                .setHandler(routingHandler
                        .wrapWithOptionsHandler()
                        .wrapWithCorsHeadersHandler()
                )
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
            if (it.requestMethod.toString().toUpperCase() == "OPTIONS") {
                it.statusCode = 204 // no content
                it.responseSender.send("")
            } else {
                this.handleRequest(it)
            }
        }
    }

}
