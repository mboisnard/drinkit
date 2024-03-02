package com.drinkit.mail

import com.drinkit.utils.isEmail

data class Email(
    val sender: Sender,
    val recipients: Recipients,
    val subject: Subject,
    val content: Content,
    val attachments: Attachments = Attachments.EMPTY,
) {
    val hasAttachments: Boolean = !attachments.isEmpty
}

data class Sender(
    val value: String,
) {
    init {
        require(value.isEmail())
    }
}

data class Recipients(
    val values: Set<Recipient>,
) {
    init {
        require(values.isNotEmpty())
    }

    fun allAsString(): Set<String> = values.map { it.value }.toSet()

    data class Recipient(
        val value: String,
    ) {
        init {
            require(value.isEmail())
        }
    }
}

data class Subject(
    val value: String,
)

data class Content(
    val format: TextFormat,
    val value: String,
) {
    val isHtmlFormat: Boolean = format == TextFormat.HTML

    enum class TextFormat {
        TXT, HTML
    }
}

data class Attachments(
    val values: Set<Attachment>,
) {
    val isEmpty: Boolean = values.isEmpty()

    data class Attachment(
        val title: String,
        val content: String, // TODO maybe octetStream
    ) {
        init {
            require(title.isNotBlank())
        }
    }

    companion object {
        val EMPTY = Attachments(emptySet())
    }
}