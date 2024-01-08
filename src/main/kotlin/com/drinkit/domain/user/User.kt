package com.drinkit.domain.user

import org.bson.types.ObjectId
import java.time.OffsetTime

data class UserId(
    val value: ObjectId,
) {
    companion object {
        fun create() = UserId(value = ObjectId())

        fun from(value: String) = UserId(value = ObjectId(value))
    }
}

data class FirstName(
    val value: String,
)

data class LastName(
    val value: String,
)

data class Email(
    val value: String,
)

enum class Role {
    ROLE_USER, ROLE_ADMIN
}

data class User(
    val id: UserId,
    val firstname: FirstName,
    val lastName: LastName,
    val email: Email,
    val lastConnection: OffsetTime,
    val roles: Set<Role>,
)