package com.drinkit.security.infra.login

import org.springframework.security.crypto.password.PasswordEncoder
import java.util.concurrent.Semaphore

/**
 * Hashes only as many passwords at once as [permits] allows, the others waiting their turn, so that the memory of the
 * hashes stays bounded whatever threads serve the requests. An Argon2id hash holds 19 MiB. On virtual threads, a hash
 * never gives its carrier back, so no more of them run at once than there are processors. On a pool of platform
 * threads, such as Tomcat's 200, a burst of logins, even with unknown usernames, which Spring hashes too, would run
 * out of memory.
 *
 * OWASP: Password Storage Cheat Sheet (Argon2id memory cost), Denial of Service Cheat Sheet (bounded resources).
 */
internal class BoundedPasswordEncoder(private val delegate: PasswordEncoder, private val permits: Semaphore) :
    PasswordEncoder {

    override fun encode(rawPassword: CharSequence?): String? = bounded { delegate.encode(rawPassword) }

    override fun matches(rawPassword: CharSequence?, encodedPassword: String?) =
        bounded { delegate.matches(rawPassword, encodedPassword) }

    override fun upgradeEncoding(encodedPassword: String?) = delegate.upgradeEncoding(encodedPassword)

    private fun <T> bounded(hash: () -> T): T {
        permits.acquire()
        try {
            return hash()
        } finally {
            permits.release()
        }
    }
}
