package util

import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.BehaviourContext
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.message.abstracts.ContentMessage
import dev.inmo.tgbotapi.types.message.content.TextContent
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.utils.italic
import dev.inmo.tgbotapi.utils.regular
import kotlinx.coroutines.flow.filter

inline fun CommonMessage<TextContent>.isNumber(onNaN: (CommonMessage<TextContent>) -> Unit): Boolean {
    val text = content.text.replace(',', '.')
    if (text.toDoubleOrNull() == null) {
        onNaN(this)
        return false
    } else {
        return true
    }
}

inline fun CommonMessage<TextContent>.isNotNumber(onNumber:  (CommonMessage<TextContent>) -> Unit): Boolean {
    val text = content.text.replace(',', '.')
    if (text.toDoubleOrNull() == null) {
        return true
    } else {
        onNumber(this)
        return false
    }
}

suspend fun BehaviourContext.waitTextMessageFromUser(userId: IdChatIdentifier) =
    waitTextMessage().filter { it.chat.id == userId }

suspend fun BehaviourContext.waitDataCallbackQueryFromUser(userId: IdChatIdentifier) =
    waitDataCallbackQuery().filter { it.user.id == userId }

suspend fun BehaviourContext.waitNumberTextMessageFromUser(userId: IdChatIdentifier) =
    waitTextMessageFromUser(userId)
        .filter { message ->
            message.isNumber {
                send(it.chat) { regular("Требуется обычное число") }
            }
        }

suspend fun BehaviourContext.waitNotNumberTextMessageFromUser(userId: IdChatIdentifier) =
    waitTextMessageFromUser(userId)
        .filter {
            it.isNotNumber { send(userId) { regular("Что?") } }
        }

suspend fun BehaviourContext.pickOutUserAnswerFromReplyMarkup(
    userMessage: ContentMessage<TextContent>,
    userId: IdChatIdentifier,
    query: DataCallbackQuery
) {
    editMessageReplyMarkup(
        userMessage,
        replyMarkup = null
    )
    send(userId) { italic(query.data) }
}