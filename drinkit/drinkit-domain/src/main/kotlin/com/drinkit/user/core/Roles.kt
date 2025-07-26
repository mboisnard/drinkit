package com.drinkit.user.core

data class Roles(
    val values: Set<Role>
) {
    enum class Role {
        ROLE_REGISTRATION_IN_PROGRESS, ROLE_USER, ROLE_ADMIN
    }

    init {
        require(values.isNotEmpty()) {
            "A role is required here"
        }
    }

    operator fun plus(role: Role) = Roles(values + role)

    fun contains(role: Role) = values.contains(role)

    fun allAsString() = values.map { it.name }.toSet()
}