package com.drinkit.user.registration

import com.drinkit.user.core.BirthDate
import com.drinkit.user.core.Email
import com.drinkit.user.core.EncodedPassword
import com.drinkit.user.core.FirstName
import com.drinkit.user.core.LastName
import com.drinkit.user.core.Roles
import com.drinkit.user.core.UserId
import java.time.LocalDateTime
import java.time.OffsetDateTime

interface NotCompletedUsers {

    fun emailExists(email: Email): Boolean

    fun findById(userId: UserId): NotCompletedUser?

    fun create(user: NotCompletedUser): UserId?

    fun update(user: NotCompletedUser): Int
}

data class NotCompletedUser(
    override val id: UserId,
    override val email: Email,
    val password: EncodedPassword?,

    val firstName: FirstName? = null,
    val lastName: LastName? = null,
    val birthDate: BirthDate? = null,

    val lastConnection: OffsetDateTime? = null,
    val roles: Roles? = null,
    val status: String,
    val completed: Boolean,
    val enabled: Boolean,
) : User {

    fun withEmailVerified() =
        copy(status = "EMAIL_VERIFIED")

    fun withUserInformation(firstName: FirstName, lastName: LastName, birthDate: BirthDate, roles: Roles) =
        copy(
            firstName = firstName,
            lastName = lastName,
            birthDate = birthDate,
            status = "USER_INFORMATION_COMPLETED",
            roles = roles,
            completed = true,
        )

    companion object {

        fun create(id: UserId, email: Email, password: EncodedPassword) =
            NotCompletedUser(
                id = id,
                email = email,
                password = password,
                status = "USER_CREATED",
                completed = false,
                enabled = true,
            )
    }
}
