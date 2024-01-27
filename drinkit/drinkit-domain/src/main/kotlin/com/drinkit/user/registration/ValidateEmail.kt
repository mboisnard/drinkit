package com.drinkit.user.registration

import com.drinkit.user.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.util.Locale

@Service
@Transactional
class ValidateEmail(
    private val userRegistrationRepository: UserRegistrationRepository,
    private val generateVerificationToken: GenerateVerificationToken,
    private val verificationTokenRepository: VerificationTokenRepository,
) {

    fun sendVerificationTokenToUser(userId: UserId, locale: Locale) {

        val notCompletedUser = userRegistrationRepository.findById(userId)
            ?: throw IllegalArgumentException("User not found")

        val token = generateVerificationToken(notCompletedUser.id)

        verificationTokenRepository.createOrUpdate(token)

    }
}