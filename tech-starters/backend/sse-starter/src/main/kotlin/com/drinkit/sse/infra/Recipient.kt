package com.drinkit.sse.infra

import com.drinkit.kotlin.sha256
import org.springframework.web.servlet.function.ServerRequest

@JvmInline
internal value class Recipient private constructor(private val key: String) {

    companion object {

        fun user(userId: String) = Recipient("user:$userId")

        // A session id is a secret: the events and the streams only hold its hash
        fun session(sessionId: String) = Recipient("session:${sessionId.sha256()}")

        /** The signed-in user, else the session the application opened, else nobody. */
        fun of(request: ServerRequest): Recipient? {
            // The user id, as security-starter names its principals
            val userId = request.principal().orElse(null)?.name
            if (userId != null) return user(userId)

            // Never a new session: anyone could fill the session store by opening the stream without a cookie
            val sessionId = request.servletRequest().getSession(false)?.id
            return sessionId?.let(::session)
        }
    }
}
