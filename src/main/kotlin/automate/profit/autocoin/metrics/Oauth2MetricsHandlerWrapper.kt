package automate.profit.autocoin.metrics

import com.timgroup.statsd.StatsDClient
import io.undertow.server.HttpHandler

fun HttpHandler.countEndpointUsage(wrapper: Oauth2MetricsHandlerWrapper): HttpHandler {
    return wrapper.wrap(this)
}


class Oauth2MetricsHandlerWrapper(private val statsdClient: StatsDClient) {

    fun wrap(next: HttpHandler): HttpHandler {
        return HttpHandler {
            val principal = it.securityContext.authenticatedAccount.principal.name
            statsdClient.count("endpointUsage", 1L, it.requestPath, principal)
            next.handleRequest(it)
        }
    }

}