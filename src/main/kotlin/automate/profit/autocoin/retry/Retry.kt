package automate.profit.autocoin.retry

import kotlinx.coroutines.delay
import mu.KotlinLogging

private val logger = KotlinLogging.logger {}

suspend fun <T>retryUntilSuccess(loggableActionName: String, action: () -> T): T {
    val delayMs = 1000
    var tryCount = 1
    while (true) {
        try {
            val result = action()
            if (tryCount > 1) {
                logger.warn { "[$loggableActionName] Success after $tryCount tries" }
            }
            return result
        } catch (e: Exception) {
            if (tryCount == 1 || tryCount % 100 == 0) {
                logger.error(e) { "[$loggableActionName] Attempt $tryCount failed, next one in ${delayMs}ms. Repeating until success and logging with ERROR level every 100th occurrence" }
            } else {
                logger.debug(e) { "Attempt $tryCount failed, next one in ${delayMs}ms. Repeating until success" }
            }
        }
        tryCount++
        delay(1000)
    }
}
