package com.drinkit.user

import java.time.LocalDateTime

data class NotCompletedUser(
    override val id: UserId,
    override val email: Email,
    val password: EncodedPassword?,

    val firstName: FirstName? = null,
    val lastName: LastName? = null,
    val birthDate: BirthDate? = null,

    val lastConnection: LocalDateTime? = null,
    val roles: Roles? = null,
    val status: String,
    val completed: Boolean,
    val enabled: Boolean,
) : User(id, email) {

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
