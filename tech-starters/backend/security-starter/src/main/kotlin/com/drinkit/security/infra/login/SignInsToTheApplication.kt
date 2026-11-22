package com.drinkit.security.infra.login

import com.drinkit.security.UserAccounts
import org.springframework.context.ApplicationListener
import org.springframework.security.authentication.event.AuthenticationSuccessEvent

// Spring publishes it for each sign-in by code or remembered, on the request thread
internal class SignInsToTheApplication(private val accounts: UserAccounts) :
    ApplicationListener<AuthenticationSuccessEvent> {

    override fun onApplicationEvent(event: AuthenticationSuccessEvent) = accounts.signedIn(event.authentication.name)
}
