package com.drinkit.security.infra.rememberme

import jakarta.servlet.http.HttpServletRequest

private const val REMEMBER_ME_REQUESTED = "com.drinkit.security.REMEMBER_ME_REQUESTED"

internal fun HttpServletRequest.requestRememberMe(requested: Boolean) {
    setAttribute(REMEMBER_ME_REQUESTED, requested)
}

internal val HttpServletRequest.rememberMeRequested get() = getAttribute(REMEMBER_ME_REQUESTED) == true
