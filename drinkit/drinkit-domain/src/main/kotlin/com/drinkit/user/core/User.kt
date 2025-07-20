package com.drinkit.user.core

import com.drinkit.documentation.cqrs.Query
import com.drinkit.documentation.event.sourcing.Projection
import com.drinkit.event.sourcing.EventsReducer
import com.drinkit.user.core.Roles.Role.ROLE_USER
import com.drinkit.user.core.UserStatus.PROFILE_COMPLETION_REQUIRED
import java.time.LocalDateTime

@Query
@Projection
data class User(
    val id: UserId,
    val email: Email,
    val password: EncodedPassword,
    val profile: ProfileInformation?,
    val lastConnection: LocalDateTime?,
    val roles: Roles,
    val status: UserStatus,
) {

    private fun apply(event: ProfileCompleted) = this.copy(
        profile = event.profile,
        status = UserStatus.ACTIVE,
        roles = Roles(setOf(ROLE_USER))
    )

    companion object {
        fun from(history: UserHistory): User {
            val reducer = EventsReducer<User, UserEvent, UserInitialized>(
                factory = User::applyInitialization
            ).register<ProfileCompleted>(User::apply)

            return reducer.reduce(history.initEvent, history.remainingEvents)
        }

        private fun applyInitialization(event: UserInitialized) = User(
            id = event.userId,
            email = event.email,
            password = event.password,
            profile = null,
            lastConnection = null,
            roles = event.roles,
            status = PROFILE_COMPLETION_REQUIRED,
        )
    }
}

enum class UserStatus {
    PROFILE_COMPLETION_REQUIRED,
    ACTIVE,
    DELETED,
}

