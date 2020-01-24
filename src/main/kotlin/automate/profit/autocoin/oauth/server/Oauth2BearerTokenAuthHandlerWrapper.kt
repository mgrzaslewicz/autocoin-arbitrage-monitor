package automate.profit.autocoin.oauth.server

import automate.profit.autocoin.api.HttpHandlerWrapper
import io.undertow.security.api.AuthenticationMode
import io.undertow.security.handlers.AuthenticationCallHandler
import io.undertow.security.handlers.AuthenticationConstraintHandler
import io.undertow.security.handlers.AuthenticationMechanismsHandler
import io.undertow.security.handlers.SecurityInitialHandler
import io.undertow.security.idm.Account
import io.undertow.security.idm.Credential
import io.undertow.security.idm.IdentityManager
import io.undertow.server.HttpHandler

fun HttpHandler.authorizeWithOauth2(oauth2BearerTokenAuthHandlerWrapper: HttpHandlerWrapper): HttpHandler {
    return oauth2BearerTokenAuthHandlerWrapper.wrap(this)
}


class Oauth2BearerTokenAuthHandlerWrapper(private val oauth2AuthenticationMechanism: Oauth2AuthenticationMechanism): HttpHandlerWrapper {

    override fun wrap(handler: HttpHandler): HttpHandler {
        return SecurityInitialHandler(
                AuthenticationMode.PRO_ACTIVE,
                object : IdentityManager {
                    override fun verify(account: Account) = account
                    override fun verify(id: String?, credential: Credential?) = null
                    override fun verify(credential: Credential?) = null
                },
                AuthenticationMechanismsHandler(
                        AuthenticationConstraintHandler(AuthenticationCallHandler(handler)),
                        listOf(oauth2AuthenticationMechanism)
                )
        )
    }

}