package com.drinkit.user

import java.time.LocalDateTime

interface CompletedUsers {

    fun findById(userId: UserId): CompletedUser?
}

data class CompletedUser(
    override val id: UserId,
    override val email: Email,
    val firstname: FirstName,
    val lastName: LastName,
    val birthDate: BirthDate?,

    val lastConnection: LocalDateTime?,
    val roles: Roles,
    val enabled: Boolean,
) : User(id, email) {
    val isAdmin = roles.values.contains(Roles.Role.ROLE_ADMIN)
}
