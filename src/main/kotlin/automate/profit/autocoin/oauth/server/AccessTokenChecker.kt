package automate.profit.autocoin.oauth.server

import automate.profit.autocoin.app.AppConfig
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.*

@JsonIgnoreProperties(ignoreUnknown = true)
data class UserAccountDto(
    val userAccountId: String
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class CheckTokenDto(
    @JsonProperty("user_name")
    val userName: String,
    val authorities: Set<String>,
    val userAccount: UserAccountDto
)

class AccessTokenChecker(
    private val httpClient: OkHttpClient,
    private val objectMapper: ObjectMapper,
    private val appConfig: AppConfig
) {
    private val base64EncodedClientIdAndSecret =
        Base64.getEncoder().encodeToString("${appConfig.arbitrageMonitorOauth2ClientId}:${appConfig.arbitrageMonitorOauth2ClientSecret}".toByteArray())

    fun checkToken(bearerToken: String): CheckTokenDto? {
        val tokenCheckResponse = httpClient.newCall(
            Request.Builder()
                .post(FormBody.Builder().add("token", bearerToken).build())
                .header("Content-Type", "application/x-www-form-urlencoded")
                .header("Authorization", "Basic $base64EncodedClientIdAndSecret")
                .url("${appConfig.oauth2ServerUrl}/oauth/check_token")
                .build()
        ).execute()
        tokenCheckResponse.use {
            val responseBody = tokenCheckResponse.body?.string() // close response body
            return when (tokenCheckResponse.code) {
                401 -> null
                200 -> {
                    objectMapper.readValue(responseBody, CheckTokenDto::class.java)
                }
                else -> null
            }
        }
    }
}
