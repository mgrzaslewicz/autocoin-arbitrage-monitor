package automate.profit.autocoin.app

import java.util.*

class AppVersion {
    private fun loadCommitId(): String? {
        val properties = Properties()
        properties.load(AppVersion::class.java.getResourceAsStream("/git.properties"))
        return properties["git.commit.id.describe"] as String?
    }

    val commitId: String? by lazy {
        try {
            loadCommitId()
        } catch (e: Exception) {
            null
        }
    }
}
