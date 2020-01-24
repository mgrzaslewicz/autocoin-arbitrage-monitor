package automate.profit.autocoin.api

import io.undertow.server.HttpHandler

interface HttpHandlerWrapper {
    fun wrap(next: HttpHandler): HttpHandler
}