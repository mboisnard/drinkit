package com.drinkit.user.registration

import com.drinkit.user.core.BirthDate
import com.drinkit.user.core.FirstName
import com.drinkit.user.core.LastName
import com.drinkit.user.core.Roles
import com.drinkit.user.core.Roles.Role
import com.drinkit.user.core.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

data class CompleteUserInformationCommand(
    val userId: UserId,
    val firstName: FirstName,
    val lastName: LastName,
    val birthDate: BirthDate,
)

@Service
class CompleteUserInformation(
    private val notCompletedUsers: NotCompletedUsers,
) {

    @Transactional
    operator fun invoke(command: CompleteUserInformationCommand) = with(command) {
        val notCompletedUser = notCompletedUsers.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        notCompletedUsers.update(
            notCompletedUser.withUserInformation(
                firstName = firstName,
                lastName = lastName,
                birthDate = birthDate,
                roles = Roles(setOf(Role.ROLE_USER, Role.ROLE_ADMIN)),
            )
        )
    }
}
