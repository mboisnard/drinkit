package com.drinkit.user.registration

abstract class UserRegistrationRepositoryContract {

    val repository: UserRegistrationRepository by lazy {
        getRepository()
    }

    abstract fun getRepository(): UserRegistrationRepository
}