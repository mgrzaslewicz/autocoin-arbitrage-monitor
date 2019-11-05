package automate.profit.autocoin.api

import automate.profit.autocoin.config.AppConfig
import io.undertow.Undertow
import io.undertow.security.handlers.AuthenticationCallHandler
import io.undertow.server.RoutingHandler

class ServerBuilder(
        private val appServerPort: Int,
        private val apiControllers: List<ApiController>
) {
    fun build(): Undertow {
        val routingHandler = RoutingHandler()
        apiControllers.forEach {
            it.apiHandlers().forEach { handler ->
                routingHandler.add(
                        handler.method(),
                        handler.urlTemplate(),
                        (handler.httpHandler())
                )
            }
        }
        return Undertow.builder()
                .addHttpListener(appServerPort, "localhost")
                .setHandler(routingHandler)
                .build()
    }
}