package automate.profit.autocoin.metrics

import automate.profit.autocoin.api.HttpHandlerWrapper
import com.timgroup.statsd.StatsDClient
import io.undertow.server.HttpHandler

fun HttpHandler.countEndpointUsage(oauth2MetricsHandlerWrapper: HttpHandlerWrapper): HttpHandler {
    return oauth2MetricsHandlerWrapper.wrap(this)
}


class Oauth2MetricsHandlerWrapper(private val statsdClient: StatsDClient): HttpHandlerWrapper {

    override fun wrap(next: HttpHandler): HttpHandler {
        return HttpHandler {
            val principal = it.securityContext.authenticatedAccount.principal.name
            statsdClient.count("endpointUsage", 1L, it.requestPath, principal)
            next.handleRequest(it)
        }
    }

}