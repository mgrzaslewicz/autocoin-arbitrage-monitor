package automate.profit.autocoin.api

import io.undertow.server.HttpHandler
import io.undertow.util.HttpString

interface ApiHandler {
    val method: HttpString
    val urlTemplate: String
    val httpHandler: HttpHandler
}