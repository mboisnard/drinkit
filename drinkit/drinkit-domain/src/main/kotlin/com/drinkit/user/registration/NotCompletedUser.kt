package com.drinkit.user.registration

import com.drinkit.user.*
import java.time.LocalDateTime

data class NotCompletedUser(
    val id: UserId,
    val email: Email,
    val password: EncodedPassword?,

    val firstname: FirstName? = null,
    val lastName: LastName? = null,
    val birthDate: BirthDate? = null,

    val lastConnection: LocalDateTime? = null,
    val roles: Roles? = null,
    val status: String,
    val completed: Boolean,
    val enabled: Boolean,
)
