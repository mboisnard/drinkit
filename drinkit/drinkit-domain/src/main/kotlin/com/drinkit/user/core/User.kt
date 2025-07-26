package com.drinkit.user.core

import com.drinkit.documentation.cqrs.Query
import com.drinkit.documentation.event.sourcing.Projection
import com.drinkit.event.sourcing.EventsReducer
import com.drinkit.user.core.Roles.Role.ROLE_ADMIN
import com.drinkit.user.core.Roles.Role.ROLE_USER
import com.drinkit.user.core.UserStatus.PROFILE_COMPLETION_REQUIRED
import java.time.OffsetDateTime

@Query
@Projection
data class User(
    val id: UserId,
    val email: Email,
    val password: EncodedPassword,
    val profile: ProfileInformation?,
    val lastConnection: OffsetDateTime?,
    val roles: Roles,
    val status: UserStatus,
    val verified: Boolean,
) {
    val enabled: Boolean = status != UserStatus.DELETED

    private fun apply(event: ProfileCompleted) = this.copy(
        profile = event.profile,
        status = UserStatus.ACTIVE,
        roles = Roles(setOf(ROLE_USER))
    )

    private fun apply(_event: Verified) = this.copy(
        verified = true,
    )

    private fun apply(_event: PromotedAsAdmin) = this.copy(
        roles = roles + ROLE_ADMIN
    )

    private fun apply(_event: Deleted) = this.copy(
        status = UserStatus.DELETED,
    )

    companion object {
        fun from(history: UserHistory): User {
            val reducer = EventsReducer<User, UserEvent, Initialized>(
                factory = User::applyInitialization
            )
                .register<ProfileCompleted>(User::apply)
                .register<Verified>(User::apply)
                .register<PromotedAsAdmin>(User::apply)
                .register<Deleted>(User::apply)

            return reducer.reduce(history.initEvent, history.remainingEvents)
        }

        private fun applyInitialization(event: Initialized) = User(
            id = event.userId,
            email = event.email,
            password = event.password,
            profile = null,
            lastConnection = null,
            roles = event.roles,
            status = PROFILE_COMPLETION_REQUIRED,
            verified = false,
        )
    }
}

enum class UserStatus {
    PROFILE_COMPLETION_REQUIRED,
    ACTIVE,
    DELETED,
}

