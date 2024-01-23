package com.drinkit.security

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Component

@Component
internal class UserPasswordEncoder: PasswordEncoder {

    private val bCryptPasswordEncoder = BCryptPasswordEncoder()

    override fun encode(rawPassword: CharSequence): String =
        bCryptPasswordEncoder.encode(rawPassword)

    override fun matches(rawPassword: CharSequence, encodedPassword: String): Boolean =
        bCryptPasswordEncoder.matches(rawPassword, encodedPassword)
}