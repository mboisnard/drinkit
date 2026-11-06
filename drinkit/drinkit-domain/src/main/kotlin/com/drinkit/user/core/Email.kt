package com.drinkit.user.core

import com.drinkit.common.Constants
import com.drinkit.kotlin.addUnless
import com.drinkit.kotlin.isEmail

data class Email(val value: String) {
    fun validate() = buildList {
        addUnless(value.isNotBlank(), "Email should not be blank")
        addUnless(value.isEmail(), "Email should be valid")
        addUnless(
            value.length <= Constants.MAX_EMAIL_LENGTH,
            "Email should have less than ${Constants.MAX_EMAIL_LENGTH} characters",
        )
    }
}
