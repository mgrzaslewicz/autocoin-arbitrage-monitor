package automate.profit.autocoin

import java.lang.System.setProperty

/**
 * Copy this file to src/main and provide settings to run
 */
fun main() {
    //    setProperty("OAUTH2_SERVER_URL", "http://localhost:9002")
    setProperty("APP_OAUTH_CLIENT_ID", "changeme")
    setProperty("APP_OAUTH_CLIENT_SECRET", "changeme")
    main(emptyArray())
}
