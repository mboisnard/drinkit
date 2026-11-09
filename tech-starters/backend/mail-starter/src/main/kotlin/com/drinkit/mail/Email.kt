package com.drinkit.mail

import com.drinkit.kotlin.isEmail

data class Email(val sender: Sender, val recipients: Recipients, val subject: Subject, val content: Content)

data class Sender(val value: String) {
    init {
        require(value.isEmail())
    }
}

data class Recipients(val values: Set<Recipient>) {
    init {
        require(values.isNotEmpty())
    }

    fun addresses(): Set<String> = values.map { it.value }.toSet()

    data class Recipient(val value: String) {
        init {
            require(value.isEmail())
        }
    }
}

data class Subject(val value: String)

data class Content(val format: TextFormat, val value: String) {
    val isHtmlFormat: Boolean = format == TextFormat.HTML

    enum class TextFormat {
        TXT,
        HTML,
    }
}
