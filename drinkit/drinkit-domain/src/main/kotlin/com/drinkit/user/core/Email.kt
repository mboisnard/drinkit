package com.drinkit.user.core

import com.drinkit.common.Constants
import com.drinkit.utils.addIfNotMatch
import com.drinkit.utils.isEmail

data class Email(
    val value: String,
) {
    fun validate() = buildList {
        addIfNotMatch(value.isNotBlank(), "Email should not be blank")
        addIfNotMatch(value.isEmail(), "Email should be valid, $value")
        addIfNotMatch(
            value.length <= Constants.MAX_EMAIL_LENGTH,
            "Email should have less than ${Constants.MAX_EMAIL_LENGTH} characters, $value"
        )
    }
}