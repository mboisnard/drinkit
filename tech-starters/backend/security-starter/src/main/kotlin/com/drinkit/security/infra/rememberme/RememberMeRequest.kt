package com.drinkit.security.infra.rememberme

import jakarta.servlet.http.HttpServletRequest

private const val REMEMBER_ME_REQUESTED = "com.drinkit.security.REMEMBER_ME_REQUESTED"

/** Records that the sign-in of this request asked to be remembered, for the remember-me services to read after it. */
internal fun HttpServletRequest.requestRememberMe(requested: Boolean) {
    setAttribute(REMEMBER_ME_REQUESTED, requested)
}

internal val HttpServletRequest.rememberMeRequested get() = getAttribute(REMEMBER_ME_REQUESTED) == true
