package util

import dev.inmo.tgbotapi.extensions.utils.textContentOrNull
import dev.inmo.tgbotapi.extensions.utils.textedContentOrNull
import dev.inmo.tgbotapi.types.message.content.MessageContent
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

enum class Maybe(val ru: String) {
    YES("Да"),
    NO("Нет")
}

val MessageContent.text: String?
    get() =
        textContentOrNull()?.text ?: (this.textedContentOrNull())?.text

fun Long.toLocalDateTime(): LocalDateTime =
    Instant.ofEpochMilli(this).atZone(ZoneId.systemDefault()).toLocalDateTime()
