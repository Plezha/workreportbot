package questions

import SheetsApi
import com.google.api.client.auth.oauth2.TokenResponseException
import dev.inmo.tgbotapi.extensions.api.edit.reply_markup.editMessageReplyMarkup
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitMessageDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.expectations.waitTextMessage
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.text
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.IdChatIdentifier
import dev.inmo.tgbotapi.types.RawChatId
import dev.inmo.tgbotapi.types.message.abstracts.CommonMessage
import dev.inmo.tgbotapi.types.queries.callback.DataCallbackQuery
import dev.inmo.tgbotapi.types.toChatId
import dev.inmo.tgbotapi.utils.*
import fullUsername
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.merge
import notifications.NotificationsManager
import storage.UserStorage
import util.*
import java.text.ParseException
import java.text.SimpleDateFormat

fun DefaultBehaviourContextWithFSM<BotState>.handleQuestionsAnswered(
    notificationsManager: NotificationsManager,
    storage: UserStorage
) {
    strictlyOn<SubmitState> { question ->
        send(question.context) { regular("Данные для отчёта:\n") }
        send(question.context) {
            italic(question.currentProgress.toString())
        }
        val isDataCorrectMessage = send(
            question.context,
            replyMarkup = inlineKeyboard {
                row { dataButton(Maybe.YES.ru, Maybe.YES.ru) }
                row { dataButton(Maybe.NO.ru, Maybe.NO.ru) }
            },
        ) {
            regular("Всё верно?")
        }

        val query = waitDataCallbackQueryFromUser(question.context).first()

        pickOutUserAnswerFromReplyMarkup(isDataCorrectMessage, question.context, query)

        if (query.data == Maybe.NO.ru) {
            val answerDescriptions = answerDescriptions(question.currentProgress.vehicleType!!)
            val dataToEditMessage = send(
                question.context,
                replyMarkup = inlineKeyboard {
                    for (description in answerDescriptions) {
                        row { dataButton(description, description) }
                    }
                },
            ) {
                regular("Выберите, что хотите изменить:")
            }
            val answerDescriptionQuery = waitDataCallbackQueryFromUser(question.context).first {
                answerDescriptions.contains(it.data)
            }
            pickOutUserAnswerFromReplyMarkup(dataToEditMessage, question.context, answerDescriptionQuery)
            val id = question.context
            val progress = question.currentProgress
            return@strictlyOn when (answerDescriptionQuery.data) {
                DateQuestion.answerDescription -> DateQuestion(id, progress, isEditing = true)
                VehicleQuestion.answerDescription -> VehicleQuestion(id, progress, isEditing = true)
                HeavyVehicleEngineHoursQuestion.answerDescription -> HeavyVehicleEngineHoursQuestion(
                    id,
                    progress,
                    isEditing = true
                )

                LightVehicleMileageQuestion.answerDescription -> LightVehicleMileageQuestion(
                    id,
                    progress,
                    isEditing = true
                )

                HoursWorkedQuestion.answerDescription -> HoursWorkedQuestion(id, progress, isEditing = true)
                ObjectDescriptionQuestion.answerDescription -> ObjectDescriptionQuestion(id, progress, isEditing = true)
                TaskDescriptionQuestion.answerDescription -> TaskDescriptionQuestion(id, progress, isEditing = true)
                FuelUsedQuestion.answerDescription -> FuelUsedQuestion(id, progress, isEditing = true)
                FuelGotQuestion.answerDescription -> FuelGotQuestion(id, progress, isEditing = true)
                MalfunctionQuestion.answerDescription -> MalfunctionQuestion(id, progress, isEditing = true)
                else -> null
            }
        } else {
            try {
                SheetsApi.appendRowDataToSheet(
                    question.currentProgress.toReportList()
                )
            } catch (e: TokenResponseException) {
                send(chatId = ChatId(RawChatId(132140512))) {
                    regular("""Юзер не смог отправить результаты из-за старости токенов. Нужно перезайти в аккаунт на сервере" +
                            |Юзер:
                            |${query.from.fullUsername()}""".trimMargin())
                }
                SheetsApi.refreshCredentials()
                SheetsApi.appendRowDataToSheet(
                    question.currentProgress.toReportList()
                )
            }

            send(question.context.toChatId()) {
                regular(
                    "Информация принята. Спасибо!"
                )
            }
            val oldUser = storage.getAllUsersAsMap()[question.context.chatId.toString()]
            oldUser?.let {
                storage.editUser(
                    oldUser.copy(lastReportSubmitted = System.currentTimeMillis())
                )
            }
            notificationsManager.rescheduleUncommitedReportReminders()
        }
        null
    }
}

@OptIn(RiskFeature::class)
fun DefaultBehaviourContextWithFSM<BotState>.handleMalfunctionQuestions() {
    strictlyOn<MalfunctionQuestion> { question ->
        val sentMessage = send(
            question.context,
            replyMarkup = inlineKeyboard {
                row { dataButton(Maybe.YES.ru, Maybe.YES.ru) }
                row { dataButton(Maybe.NO.ru, Maybe.NO.ru) }
            },
        ) {
            regular("Наличие неисправности:")
        }
        val query = waitDataCallbackQueryFromUser(question.context).first()

        pickOutUserAnswerFromReplyMarkup(sentMessage, question.context, query)

        if (query.data == Maybe.NO.ru) {
            question.currentProgress.malfunctions = null
            return@strictlyOn SubmitState(question.context, question.currentProgress, query.user.id)
        }

        send(
            question.context,
        ) {
            regular("Опишите неисправность. Далее свяжемся с Вами и уточним")
        }

        val userMessage = waitTextMessage()
            .filter { it.chat.id == question.context }
            .filter { message ->
                question.currentProgress.malfunctions = message.text ?: ""
                true
            }.first()

        if (question.isEditing) {
            SubmitState(question.context, question.currentProgress, userMessage.chat.id)
        } else {
            SubmitState(question.context, question.currentProgress, userMessage.chat.id)
        }
    }
}

@OptIn(RiskFeature::class)
fun DefaultBehaviourContextWithFSM<BotState>.handleFuelGotQuestion() {
    strictlyOn<FuelGotQuestion> { question ->
        send(
            question.context,
        ) {
            regular("Топлива ") + bold("получено") + regular(" (л)")
        }
        val contentMessage = waitNumberTextMessageFromUser(question.context).first()
        question.currentProgress.fuelGot = contentMessage.text

        if (question.isEditing) {
            SubmitState(question.context, question.currentProgress, contentMessage.chat.id)
        } else {
            MalfunctionQuestion(question.context, question.currentProgress)
        }
    }
}

@OptIn(RiskFeature::class)
fun DefaultBehaviourContextWithFSM<BotState>.handleFuelUsedQuestion() {
    strictlyOn<FuelUsedQuestion> { question ->
        send(
            question.context,
        ) {
            regular("Топлива ") + bold("потрачено") + regular(" (л)")
        }
        val contentMessage = waitNumberTextMessageFromUser(question.context).first()
        question.currentProgress.fuelUsed = contentMessage.text

        if (question.isEditing) {
            SubmitState(question.context, question.currentProgress, contentMessage.chat.id)
        } else {
            FuelGotQuestion(question.context, question.currentProgress)
        }
    }
}


@OptIn(RiskFeature::class)
fun DefaultBehaviourContextWithFSM<BotState>.handleTaskDescription() {
    strictlyOn<TaskDescriptionQuestion> { question ->
        val message =
            if (question.currentProgress.vehicleType == VehicleType.HEAVY) {
                """Описание выполненной работы (кратко):
                    |Пример: Планировка основания""".trimMargin()
            } else {
                """Описание маршрута (кратко):
                    |Пример: База - Объект""".trimMargin()
            }
        send(
            question.context,
        ) {
            regular(message)
        }
        val contentMessage = waitNotNumberTextMessageFromUser(question.context).first()

        question.currentProgress.taskDescription = contentMessage.text

        if (question.isEditing) {
            SubmitState(question.context, question.currentProgress, contentMessage.chat.id)
        } else {
            FuelUsedQuestion(question.context, question.currentProgress)
        }
    }
}

fun DefaultBehaviourContextWithFSM<BotState>.handleObjectDescription() {
    strictlyOn<ObjectDescriptionQuestion> { question ->
        val sentMessage = send(
            question.context,
            replyMarkup = inlineKeyboard {
                for (obj in objectDescriptions) {
                    row { dataButton(obj, obj) }
                }
            }
        ) {
            regular("Место работы (объект):")
        }
        val query = waitDataCallbackQueryFromUser(question.context).first()

        pickOutUserAnswerFromReplyMarkup(sentMessage, question.context, query)

        question.currentProgress.objectDescription = query.data

        if (question.isEditing) {
            SubmitState(question.context, question.currentProgress, query.user.id)
        } else {
            TaskDescriptionQuestion(question.context, question.currentProgress)
        }
    }
}

@OptIn(RiskFeature::class)
fun DefaultBehaviourContextWithFSM<BotState>.handleWorkingHours() {
    strictlyOn<HoursWorkedQuestion> { question ->
        send(
            question.context,
        ) {
            regular("Количество ") + bold("рабочих часов") + regular(" за сегодня:")
        }
        val contentMessage = waitNumberTextMessageFromUser(question.context).first()

        question.currentProgress.hoursWorked = contentMessage.text

        if (question.isEditing) {
            SubmitState(question.context, question.currentProgress, contentMessage.chat.id)
        } else {
            ObjectDescriptionQuestion(question.context, question.currentProgress)
        }
    }
}

@OptIn(RiskFeature::class)
fun DefaultBehaviourContextWithFSM<BotState>.handleVehicleUsage() {
    strictlyOn<HeavyVehicleEngineHoursQuestion> { question ->
        send(
            question.context,
        ) {
            regular("Сколько моточасов за отчетный день наработано:")
        }
        val contentMessage = waitNumberTextMessageFromUser(question.context).first()

        question.currentProgress.engineHours = contentMessage.text

        if (question.isEditing) {
            SubmitState(question.context, question.currentProgress, contentMessage.chat.id)
        } else {
            HoursWorkedQuestion(question.context, question.currentProgress)
        }
    }
    strictlyOn<LightVehicleMileageQuestion> { question ->
        send(
            question.context,
        ) {
            regular("Пробег в км за отчетный день:")
        }
        val contentMessage = waitNumberTextMessageFromUser(question.context).first()

        question.currentProgress.mileage = contentMessage.text

        if (question.isEditing) {
            SubmitState(question.context, question.currentProgress, contentMessage.chat.id)
        } else {
            HoursWorkedQuestion(question.context, question.currentProgress)
        }
    }
}

fun DefaultBehaviourContextWithFSM<BotState>.handleVehicle() {
    strictlyOn<VehicleQuestion> { question ->
        val vehicleTypeMessage = send(
            question.context,
            replyMarkup = inlineKeyboard {
                for (vehicleType in VehicleType.entries) {
                    row { dataButton(vehicleType.ru, vehicleType.ru) }
                }
            },
        ) {
            regular("Выберите ") + bold("тип") + regular(" своей техники:")
        }

        val vehicleTypeQuery = waitDataCallbackQueryFromUser(question.context)
            .first { query ->
                query.data in VehicleType.entries.map { it.ru }
            }

        pickOutUserAnswerFromReplyMarkup(vehicleTypeMessage, question.context, vehicleTypeQuery)

        val vehicleType = when (vehicleTypeQuery.data) {
            VehicleType.HEAVY.ru -> VehicleType.HEAVY
            else -> VehicleType.LIGHT
        }
        question.currentProgress.vehicleType = vehicleType

        val vehicleMessage = send(
            question.context,
            replyMarkup = inlineKeyboard {
                for (vehicle in vehicleType.list) {
                    row { dataButton(vehicle, vehicle) }
                }
            },
        ) {
            regular("Выберите ") + bold("свою технику") + regular(" из списка:")
        }

        val vehicleQuery = waitMessageDataCallbackQuery()
            .filter { it.user.id == question.context }
            .first { query ->
                query.data in vehicleType.list
            }

        editMessageReplyMarkup(
            vehicleMessage,
            replyMarkup = null
        )
        send(question.context) { italic(vehicleQuery.data) }

        question.currentProgress.vehicle = vehicleQuery.data
        if (question.isEditing) {
            if (vehicleType == VehicleType.HEAVY && question.currentProgress.engineHours == null) {
                question.currentProgress.mileage = null
                HeavyVehicleEngineHoursQuestion(question.context, question.currentProgress, isEditing = true)
            } else if (vehicleType == VehicleType.LIGHT && question.currentProgress.mileage == null) {
                question.currentProgress.engineHours = null
                LightVehicleMileageQuestion(question.context, question.currentProgress, isEditing = true)
            } else {
                SubmitState(question.context, question.currentProgress, vehicleQuery.user.id)
            }
        } else {
            when (vehicleType) {
                VehicleType.HEAVY -> HeavyVehicleEngineHoursQuestion(question.context, question.currentProgress)
                VehicleType.LIGHT -> LightVehicleMileageQuestion(question.context, question.currentProgress)
            }
        }
    }
}

@OptIn(RiskFeature::class)
fun DefaultBehaviourContextWithFSM<BotState>.handleDate() {
    val dateFormat = SimpleDateFormat("dd.MM.yy").apply {
        isLenient = false
    }
    strictlyOn<DateQuestion> { question ->
        val nowDateString = dateFormat.format(System.currentTimeMillis())
        val sentMessage = send(
            question.context,
            replyMarkup = inlineKeyboard {
                row { dataButton(nowDateString, nowDateString) }
            }
        ) {
            regular(
                """Дата дд.мм.гг
                    |Пример: $nowDateString""".trimMargin()
            )
        }

        val messageOrQuery =
            merge(waitTextMessageFromUser(question.context), waitDataCallbackQueryFromUser(question.context))
                .first { messageOrQuery ->
                    val text = when (messageOrQuery) {
                        is CommonMessage<*> -> messageOrQuery.text
                        is DataCallbackQuery -> messageOrQuery.data
                        else -> ""
                    }
                    try {
                        dateFormat.parse(text)
                        true
                    } catch (e: ParseException) {
                        if (messageOrQuery is CommonMessage<*>) send(messageOrQuery.chat, "Неверный формат даты")
                        false
                    }
                }

        when (messageOrQuery) {
            is DataCallbackQuery -> pickOutUserAnswerFromReplyMarkup(sentMessage, question.context, messageOrQuery)
        }
        val date = when (messageOrQuery) {
            is DataCallbackQuery -> messageOrQuery.data
            is CommonMessage<*> -> messageOrQuery.text
            else -> ""
        }
        val id: IdChatIdentifier = when (messageOrQuery) {
            is DataCallbackQuery -> messageOrQuery.user.id
            is CommonMessage<*> -> messageOrQuery.chat.id
            else -> throw IllegalStateException(
                "Got not message neither query from user while waiting for message or query"
            )
        }

        question.currentProgress.date = dateFormat.format(
            dateFormat.parse(date)
        ) // 20.04.2024 -> 20.04.24

        if (question.isEditing) {
            SubmitState(question.context, question.currentProgress, id)
        } else {
            VehicleQuestion(question.context, question.currentProgress)
        }
    }
}
