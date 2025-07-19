package com.drinkit.user.registration

import com.drinkit.user.core.BirthDate
import com.drinkit.user.core.Email
import com.drinkit.user.core.FirstName
import com.drinkit.user.core.LastName
import com.drinkit.user.core.Roles
import com.drinkit.user.core.UserId
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
) : User {
    val isAdmin = roles.values.contains(Roles.Role.ROLE_ADMIN)
}
