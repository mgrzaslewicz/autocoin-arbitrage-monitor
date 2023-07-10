package automate.profit.autocoin

import automate.profit.autocoin.app.AppContext
import automate.profit.autocoin.app.AppStarter
import automate.profit.autocoin.app.config.ConfigLoader.Companion.loadConfig
import mu.KotlinLogging
import java.net.SocketAddress
import kotlin.system.measureTimeMillis

private val logger = KotlinLogging.logger { }

/**
 * For local development run with PROFILE=dev
 * either env variable or property
 */
fun main(args: Array<String>) {
    var address: SocketAddress? = null
    val bootTimeMillis = measureTimeMillis {
        val config = loadConfig()
        logger.info { "Config: $config" }
        val appContext = AppContext(config)
        val appStarter = AppStarter(appContext)
        address = appStarter.start().serverAddress
    }
    logger.info { "Started in $bootTimeMillis ms, available at $address" }
}
