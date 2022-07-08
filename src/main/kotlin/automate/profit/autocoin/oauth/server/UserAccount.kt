package automate.profit.autocoin.oauth.server

import io.undertow.security.idm.Account
import java.security.Principal

fun CheckTokenDto.toUserAccount() = UserAccount(userName, userAccount.userAccountId, authorities)

class UserAccount(
        private val userName: String,
        private val userAccountId: String,
        private val authorities: Set<String>
) : Account {
    override fun getRoles() = authorities
    override fun getPrincipal() = Principal { userAccountId }
}