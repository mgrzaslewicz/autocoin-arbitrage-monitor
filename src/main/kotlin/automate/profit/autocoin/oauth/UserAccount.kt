package automate.profit.autocoin.oauth

import io.undertow.security.idm.Account
import java.security.Principal

fun CheckTokenDto.toUserAccount() = UserAccount(userName, authorities)

class UserAccount(private val userName: String, private val authorities: Set<String>) : Account {
    override fun getRoles() = authorities
    override fun getPrincipal() = Principal { userName }
}