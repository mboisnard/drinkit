package com.drinkit.featuremanagement.infra

import com.drinkit.featuremanagement.FlagSubject
import com.drinkit.featuremanagement.SubjectAttributes
import com.drinkit.security.CurrentUser
import com.drinkit.security.CurrentVisitor

internal class RequestSubject(
    private val currentUser: CurrentUser,
    private val currentVisitor: CurrentVisitor,
    private val attributes: SubjectAttributes,
) {

    fun get(): FlagSubject {
        val user = currentUser.orNull() ?: return FlagSubject(currentVisitor.id())
        return FlagSubject(user.id, user.roles.associateWith { "true" } + attributes.of(user))
    }
}
