package automate.profit.autocoin.api

import io.undertow.server.HttpHandler
import io.undertow.util.HttpString

interface ApiHandler {
    fun method(): HttpString
    fun urlTemplate(): String
    fun httpHandler(): HttpHandler
}