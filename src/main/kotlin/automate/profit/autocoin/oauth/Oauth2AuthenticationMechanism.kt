package automate.profit.autocoin.oauth

import io.undertow.security.api.AuthenticationMechanism
import io.undertow.security.api.AuthenticationMechanism.AuthenticationMechanismOutcome
import io.undertow.security.api.AuthenticationMechanism.AuthenticationMechanismOutcome.*
import io.undertow.security.api.AuthenticationMechanism.ChallengeResult.NOT_SENT
import io.undertow.security.api.SecurityContext
import io.undertow.server.HttpServerExchange
import io.undertow.util.HeaderMap

class Oauth2AuthenticationMechanism(private val accessTokenChecker: AccessTokenChecker) : AuthenticationMechanism {
    private val mechanismName = "Oauth2Authentication"
    override fun sendChallenge(exchange: HttpServerExchange?, securityContext: SecurityContext?) = NOT_SENT!!

    override fun authenticate(exchange: HttpServerExchange, securityContext: SecurityContext?): AuthenticationMechanismOutcome {
        val account = securityContext?.authenticatedAccount
        if (account != null) {
            return AUTHENTICATED;
        }
        val bearerToken = getBearerToken(exchange.requestHeaders)
        return if (bearerToken != null) {
            try {
                val checkTokenDto = accessTokenChecker.checkToken(bearerToken)
                if (checkTokenDto != null) {
                    securityContext?.authenticationComplete(checkTokenDto.toUserAccount(), mechanismName, false);
                    AUTHENTICATED
                } else {
                    NOT_AUTHENTICATED
                }
            } catch (e: Exception) {
                NOT_AUTHENTICATED
            }
        } else {
            return NOT_ATTEMPTED;
        }
    }

    private fun getBearerToken(requestHeaders: HeaderMap): String? {
        val bearerToken = requestHeaders.getFirst("Authorization")
        return if (!bearerToken.isNullOrBlank() && bearerToken.contains(" ")) {
            bearerToken.split(" ")[1]
        } else null
    }

}