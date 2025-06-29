package com.drinkit.user.core

import com.drinkit.common.Constants
import com.drinkit.utils.isEmail

data class Email(
    val value: String,
) {
    init {
        require(
            value.isNotBlank() &&
                value.isEmail() &&
                value.length <= Constants.MAX_EMAIL_LENGTH
        ) {
            "Email should be valid and have less than ${Constants.MAX_EMAIL_LENGTH} characters. Given value: $value"
        }
    }
}