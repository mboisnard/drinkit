package com.drinkit.user.registration

internal class InMemoryUserRegistrationRepositoryTest: UserRegistrationRepositoryContract() {

    override fun fetchRepository(): UserRegistrationRepository =
        InMemoryUserRegistrationRepository()
}