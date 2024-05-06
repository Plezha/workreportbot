import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.bot.setMyCommands
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.behaviour_builder.telegramBotWithBehaviourAndFSMAndStartLongPolling
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.command
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onContentMessage
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onDataCallbackQuery
import dev.inmo.tgbotapi.extensions.behaviour_builder.triggers_handling.onUnhandledCommand
import dev.inmo.tgbotapi.extensions.utils.extensions.raw.from
import dev.inmo.tgbotapi.types.BotCommand
import dev.inmo.tgbotapi.utils.PreviewFeature
import dev.inmo.tgbotapi.utils.RiskFeature
import dev.inmo.tgbotapi.utils.botCommand
import dev.inmo.tgbotapi.utils.regular
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import notifications.NotificationsManager
import questions.*
import storage.User
import storage.UserStorage

private const val botToken = ""

@OptIn(PreviewFeature::class)
suspend fun main(args: Array<String>) {
    val storage = UserStorage()

    telegramBotWithBehaviourAndFSMAndStartLongPolling<BotState>(
        token = botToken,
        scope = CoroutineScope(Dispatchers.IO),
        onStateHandlingErrorHandler = { state, e ->
            println("Thrown an error on $state")
            e.printStackTrace()
            if (e is CommonRequestException) {
                // Most likely bot is blocked, and that won't fix itself
                null
            } else {
                state
            }
        },
    ) {
        val notificationsManager = NotificationsManager(this, storage)

        handleDate()
        handleVehicle()
        handleVehicleUsage()
        handleWorkingHours()
        handleObjectDescription()
        handleTaskDescription()
        handleFuelGotQuestion()
        handleFuelUsedQuestion()
        handleMalfunctionQuestions()
        handleQuestionsAnswered(notificationsManager, storage)

        handleStartCommand(notificationsManager, storage)

        setMyCommands(
            BotCommand("start", "Начать создание отчёта"),
        )

        onUnhandledCommand {
            send(it.chat.id) {
                regular("Используйте ") + botCommand("start") + regular(" для создания отчёта")
            }
        }

        onDataCallbackQuery {
            when (it.data) {
                "Я отдыхал" ->
                    send(it.user) { regular("Информация принята. Спасибо!") }

                "Я работал" ->
                    startChain(
                        DateQuestion(
                            it.user.id,
                            Answers(
                                it.from.fullUsername()
                            )
                        )
                    )
            }
        }

        onContentMessage {
            println("New ContentMessage: $it")
        }
    }.second.join()
}

@OptIn(RiskFeature::class)
private suspend fun DefaultBehaviourContextWithFSM<BotState>.handleStartCommand(
    notificationsManager: NotificationsManager,
    storage: UserStorage
) {
    command(
        "start"
    ) {
        val userId = it.chat.id.chatId.toString()

        val users = storage.getAllUsersAsMap()
        if (users.containsKey(userId)) {
            storage.editUser(
                users[userId]!!.copy(lastReportStarted = System.currentTimeMillis())
            )
        } else {
            storage.addUser(
                User(userId, System.currentTimeMillis(), 0L)
            )
        }

        notificationsManager.rescheduleUncommitedReportReminders()
        startChain(
            DateQuestion(
                it.chat.id,
                Answers(
                    it.from?.fullUsername() ?: "Not a user"
                )
            )
        )
    }
}

fun dev.inmo.tgbotapi.types.chat.User.fullUsername(): String =
    """${username?.toString() ?: "Account hidden"}
    |$firstName $lastName""".trimMargin()