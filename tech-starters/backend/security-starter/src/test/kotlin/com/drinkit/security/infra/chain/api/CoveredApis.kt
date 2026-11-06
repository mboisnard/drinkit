package com.drinkit.security.infra.chain.api

import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestMethod

internal interface CellarsApi {

    @RequestMapping(method = [RequestMethod.GET], value = ["/api/cellars/{cellarId}"])
    fun cellar(
        @PathVariable cellarId: String,
    ): String

    @RequestMapping(method = [RequestMethod.DELETE], value = ["/api/cellars/{cellarId}"])
    fun deleteCellar(
        @PathVariable cellarId: String,
    )
}

@RequestMapping("/api/public")
internal interface PublicApi {

    @GetMapping("/ping")
    fun ping()
}

internal interface AuthApi {

    @PostMapping("/api/auth/code")
    fun requestSignInCode()

    @PostMapping("/api/auth/code/sign-in")
    fun signInWithCode()
}

internal interface UnruledApi {

    @GetMapping("/api/unruled")
    fun unruled()

    @DeleteMapping("/api/public/ping")
    fun deletePing()
}
