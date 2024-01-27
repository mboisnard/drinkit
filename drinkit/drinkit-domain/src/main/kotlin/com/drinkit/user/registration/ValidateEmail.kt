package com.drinkit.user.registration

import com.drinkit.common.AbstractId
import com.drinkit.common.IdGenerator
import com.drinkit.common.isId
import com.drinkit.user.UserId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

data class VerificationToken(
    val id: VerificationTokenId,
    val userId: UserId,
    val token: String,
    val expiryDate: LocalDateTime,
) {
    data class VerificationTokenId(
        override val value: String
    ): AbstractId(value) {
        init {
            require(value.isId())
        }

        companion object {
            fun create(generator: IdGenerator) = VerificationTokenId(value = generator.createNewId())
        }
    }
}

@Service
@Transactional
class ValidateEmail(
    private val idGenerator: IdGenerator,
    private val userRegistrationRepository: UserRegistrationRepository,
    private val verificationTokenRepository: VerificationTokenRepository,
) {

    fun sendVerificationTokenToUser(userId: UserId) {

        val notCompletedUser = userRegistrationRepository.findById(userId)
            ?: throw IllegalArgumentException("Not completed user not found")


    }
}