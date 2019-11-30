package automate.profit.autocoin.oauth.client

import automate.profit.autocoin.config.AppConfig
import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.ObjectMapper
import okhttp3.FormBody
import okhttp3.OkHttpClient
import okhttp3.Request

@JsonIgnoreProperties(ignoreUnknown = true)
data class TokenResponseDto(
        @JsonProperty("access_token")
        val accessToken: String
)

class ClientCredentialsAccessTokenProvider(
        private val httpClient: OkHttpClient,
        private val objectMapper: ObjectMapper,
        private val appConfig: AppConfig
) : AccessTokenProvider {
    private var lastToken: TokenResponseDto? = null

    private fun requestBodyTemplate() = FormBody.Builder()
            .add("client_id", appConfig.arbitrageMonitorOauth2ClientId)
            .add("client_secret", appConfig.arbitrageMonitorOauth2ClientSecret)
            .add("scopes", "API")

    private fun requestToken(): TokenResponseDto {
        val formBody = requestBodyTemplate()
                .add("grant_type", "client_credentials")
                .build()
        val tokenResponse = httpClient.newCall(Request.Builder()
                .post(formBody)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .url("${appConfig.oauth2ServerUrl}/oauth/token")
                .build()
        ).execute()
        tokenResponse.use {
            check(tokenResponse.code != 401) { "Could not get access token" }
            val body = tokenResponse.body?.string()
            return objectMapper.readValue(body, TokenResponseDto::class.java)
        }
    }

    override fun token(): String? {
        if (lastToken == null) {
            lastToken = requestToken()
        }
        return lastToken?.accessToken
    }

    /**
     * Client credentials grant type has no refresh_token because it needs no user credentials, just request new token
     */
    override fun refreshToken(): String? {
        lastToken = requestToken()
        return token()
    }
}