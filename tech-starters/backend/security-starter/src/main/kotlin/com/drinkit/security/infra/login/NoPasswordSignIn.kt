package com.drinkit.security.infra.login

import org.springframework.security.authentication.AuthenticationProvider
import org.springframework.security.core.Authentication

/** Its presence alone keeps Spring Boot from creating a user with a generated password: no sign-in has one here. */
internal class NoPasswordSignIn : AuthenticationProvider {

    override fun authenticate(authentication: Authentication): Authentication? = null

    override fun supports(authentication: Class<*>) = false
}
