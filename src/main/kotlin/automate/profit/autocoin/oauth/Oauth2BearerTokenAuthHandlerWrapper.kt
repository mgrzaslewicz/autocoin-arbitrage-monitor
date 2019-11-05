package automate.profit.autocoin.oauth

import io.undertow.security.api.AuthenticationMode
import io.undertow.security.handlers.AuthenticationCallHandler
import io.undertow.security.handlers.AuthenticationConstraintHandler
import io.undertow.security.handlers.AuthenticationMechanismsHandler
import io.undertow.security.handlers.SecurityInitialHandler
import io.undertow.security.idm.Account
import io.undertow.security.idm.Credential
import io.undertow.security.idm.IdentityManager
import io.undertow.server.HttpHandler

fun HttpHandler.authorizeWithOauth2(wrapper: Oauth2BearerTokenAuthHandlerWrapper): HttpHandler {
    return wrapper.wrap(this)
}


class Oauth2BearerTokenAuthHandlerWrapper(private val oauth2AuthenticationMechanism: Oauth2AuthenticationMechanism) {

    fun wrap(handler: HttpHandler): HttpHandler {
        return SecurityInitialHandler(
                AuthenticationMode.PRO_ACTIVE,
                object: IdentityManager {
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