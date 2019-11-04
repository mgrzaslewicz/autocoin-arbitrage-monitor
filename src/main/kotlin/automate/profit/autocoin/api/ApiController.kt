package automate.profit.autocoin.api

interface ApiController {
    fun apiHandlers(): List<ApiHandler>
}