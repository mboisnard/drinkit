package com.drinkit.user

import com.drinkit.generated.jooq.tables.User.Companion.USER
import com.drinkit.generated.jooq.tables.records.UserRecord
import com.drinkit.user.core.BirthDate
import com.drinkit.user.core.Email
import com.drinkit.user.core.EncodedPassword
import com.drinkit.user.core.FirstName
import com.drinkit.user.core.LastName
import com.drinkit.user.core.ProfileInformation
import com.drinkit.user.core.Roles
import com.drinkit.user.core.Roles.Role.ROLE_ADMIN
import com.drinkit.user.core.Roles.Role.ROLE_REGISTRATION_IN_PROGRESS
import com.drinkit.user.core.Roles.Role.ROLE_USER
import com.drinkit.user.core.User
import com.drinkit.user.core.UserId
import com.drinkit.user.core.UserStatus
import com.drinkit.user.spi.Users
import io.github.oshai.kotlinlogging.KotlinLogging
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.Clock
import java.time.LocalDate
import java.time.OffsetDateTime

@Repository
internal class JooqUsersRepository(
    private val dsl: DSLContext,
    private val clock: Clock,
): Users {

    private val logger = KotlinLogging.logger {}

    override fun saveOrUpdate(user: User): User {
        val query = dsl.insertInto(USER)
            .set(USER.ID, user.id.value)
            .set(USER.EMAIL, user.email.value)
            .set(USER.PASSWORD, user.password.value)
            .set(USER.STATUS, user.status.name)
            .set(USER.VERIFIED, user.verified)
            .set(USER.ENABLED, user.enabled)
            .set(USER.ROLES, user.roles.allAsString().toTypedArray())
            .set(USER.FIRSTNAME, user.profile?.firstName?.value)
            .set(USER.LASTNAME, user.profile?.lastName?.value)
            .set(USER.BIRTHDATE, user.profile?.birthDate?.value)
            .set(USER.MODIFIED, OffsetDateTime.now(clock))
            .onConflict(USER.ID)
            .doUpdate()
            .set(USER.EMAIL, user.email.value)
            .set(USER.PASSWORD, user.password.value)
            .set(USER.STATUS, user.status.name)
            .set(USER.VERIFIED, user.verified)
            .set(USER.ENABLED, user.enabled)
            .set(USER.ROLES, user.roles.allAsString().toTypedArray())
            .set(USER.FIRSTNAME, user.profile?.firstName?.value)
            .set(USER.LASTNAME, user.profile?.lastName?.value)
            .set(USER.BIRTHDATE, user.profile?.birthDate?.value)
            .set(USER.MODIFIED, OffsetDateTime.now(clock))

        query.execute()

        return user
    }

    override fun findEnabledBy(userId: UserId): User? {
        val query = dsl.selectFrom(USER)
            .where(USER.ID.eq(userId.value))
            .and(USER.ENABLED.eq(true))

        return query.fetchOne { it.toDomain() }
    }

    override fun exists(email: Email): Boolean {
        val count = dsl.fetchCount(USER, USER.EMAIL.eq(email.value))
        return count != 0
    }

    private fun UserRecord.toDomain() =
        User(
            id = UserId(id),
            email = Email(email),
            password = EncodedPassword(password),
            profile = toProfile(firstname, lastname, birthdate),
            lastConnection = lastconnection,
            roles = toRoles(roles),
            status = toStatus(status),
            verified = verified,
        )

    private fun toProfile(firstname: String?, lastname: String?, birthdate: LocalDate?): ProfileInformation? {
        if (firstname == null || lastname == null)
            return null

        return ProfileInformation(
            firstName = FirstName(firstname),
            lastName = LastName(lastname),
            birthDate = birthdate?.let { BirthDate(it) },
        )
    }

    private fun toRoles(rolesAsString: Array<String?>): Roles {
        val roles = rolesAsString.mapNotNull {
            when (it) {
                ROLE_REGISTRATION_IN_PROGRESS.name -> ROLE_REGISTRATION_IN_PROGRESS
                ROLE_USER.name -> ROLE_USER
                ROLE_ADMIN.name -> ROLE_ADMIN
                else -> {
                    logger.warn { "Unknown role: $it, skipping it"}
                    null
                }
            }
        }.toSet()

        return Roles(roles)
    }

    private fun toStatus(statusAsString: String): UserStatus = when (statusAsString) {
        UserStatus.ACTIVE.name -> UserStatus.ACTIVE
        UserStatus.PROFILE_COMPLETION_REQUIRED.name -> UserStatus.PROFILE_COMPLETION_REQUIRED
        UserStatus.DELETED.name -> UserStatus.DELETED
        else -> throw IllegalArgumentException("Unknown user status: $statusAsString")
    }
}