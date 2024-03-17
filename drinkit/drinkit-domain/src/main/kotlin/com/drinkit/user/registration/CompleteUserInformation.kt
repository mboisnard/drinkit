package com.drinkit.user.registration

import com.drinkit.user.BirthDate
import com.drinkit.user.FirstName
import com.drinkit.user.LastName
import com.drinkit.user.Roles
import com.drinkit.user.Roles.Role
import com.drinkit.user.UserId
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
) : RegistrationStep {

    @Transactional
    operator fun invoke(command: CompleteUserInformationCommand) = with(command) {
        val notCompletedUser = notCompletedUsers.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        notCompletedUsers.update(
            notCompletedUser.copy(
                firstname = firstName,
                lastName = lastName,
                birthDate = birthDate,
                status = status(),
                roles = Roles(setOf(Role.ROLE_USER, Role.ROLE_ADMIN)),
                completed = true,
            )
        )
    }

    override fun status(): String = "USER_INFORMATION_COMPLETED"
}
