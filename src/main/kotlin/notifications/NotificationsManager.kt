package notifications

import dev.inmo.tgbotapi.bot.exceptions.CommonRequestException
import dev.inmo.tgbotapi.extensions.api.send.send
import dev.inmo.tgbotapi.extensions.behaviour_builder.DefaultBehaviourContextWithFSM
import dev.inmo.tgbotapi.extensions.utils.types.buttons.dataButton
import dev.inmo.tgbotapi.extensions.utils.types.buttons.inlineKeyboard
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.utils.regular
import dev.inmo.tgbotapi.utils.row
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import questions.BotState
import storage.UserStorage
import util.toLocalDateTime
import java.util.*

private const val hour = 60 * 60 * 1000L
private const val day = 24 * hour

class NotificationsManager(
    private val bot: DefaultBehaviourContextWithFSM<BotState>,
    private val storage: UserStorage
) {
    private val users
        get() = storage.getAllUsers()
    private val notificationsScope = CoroutineScope(Dispatchers.IO)
    private val dailyNotificationsTimer = Timer()
    private var uncommitedReportsRemindersTimer = Timer()

    init {
        scheduleDailyNotifications()
        scheduleUncommitedReportReminders()
    }

    fun rescheduleUncommitedReportReminders() {
        println("Canceling uncommited reports reminders")
        uncommitedReportsRemindersTimer.cancel()
        uncommitedReportsRemindersTimer = Timer()
        scheduleUncommitedReportReminders()
    }

    private fun scheduleUncommitedReportReminders() {
        for (user in users) {
            val currentTime = System.currentTimeMillis()
            val currentLocalDateTime = currentTime.toLocalDateTime()
//            println("${user.lastReportStartedLocalDateTime.dayOfYear} == ${currentLocalDateTime.dayOfYear}")
//            println("${user.lastReportSubmitted} < ${user.lastReportStarted}")
//            println("${currentTime} - ${user.lastReportStarted} < $hour")
            if (
                user.lastReportStartedLocalDateTime.dayOfYear == currentLocalDateTime.dayOfYear && // Started today
                user.lastReportSubmitted < user.lastReportStarted && // Not submitted yet
                currentTime - user.lastReportStarted < hour // Started less than hour ago
            ) {
                val notificationTime = user.lastReportStarted + hour
                val delay = notificationTime - currentTime

                println("Scheduling reminder for ${user.id} at ${notificationTime.toLocalDateTime()}")
                uncommitedReportsRemindersTimer.schedule(
                    object : TimerTask() {
                        override fun run() {
                            sendUncommitedReportReminder(user.chatId)
                        }
                    }, delay
                )
            }
        }
    }

    private fun sendUncommitedReportReminder(user: ChatId) {
        notificationsScope.launch {
            bot.send(user) {
                regular("Напоминание: Вы ещё не ответили на все вопросы для отчёта")
            }
        }
    }

    private fun scheduleDailyNotifications() {
        val notificationTime = Calendar.getInstance().apply {
//            add(Calendar.MINUTE, 1)
            set(Calendar.HOUR_OF_DAY, 20) // Set the hour of the day
            set(Calendar.MINUTE, 0) // Set the minute
            set(Calendar.SECOND, 0) // Set the second
        }
        // Check if the notification time has already passed for today
        if (notificationTime.timeInMillis < System.currentTimeMillis()) {
            // If so, schedule the notification for the next day
            notificationTime.add(Calendar.DAY_OF_MONTH, 1)
        }
        val delay = notificationTime.timeInMillis - System.currentTimeMillis()
        println("Scheduling daily notifications for all users at $notificationTime")

        dailyNotificationsTimer.scheduleAtFixedRate(
            object : TimerTask() {
                override fun run() {
                    sendDailyNotifications()
                }
            }, delay, day
        )
    }

    private fun sendDailyNotifications() {
        notificationsScope.launch {
            val currentLocalDateTime = System.currentTimeMillis().toLocalDateTime()
            for (user in users) {
                println("Trying to send daily notification to ${user.id}")
                if (user.lastReportSubmittedLocalDateTime.dayOfYear < currentLocalDateTime.dayOfYear) {
                    try {
                        sendDailyNotification(user.chatId)
                        println("Sent notification to ${user.id}")
                    } catch (e: CommonRequestException) {
                        println("Failed send notification to ${user.id}")
                    }
                }
                else {
                    println("Didn't send notification to ${user.id} - user already sent a report today")
                }
            }
        }
    }

    private suspend fun sendDailyNotification(user: ChatId) {
        bot.send(user,
            replyMarkup = inlineKeyboard {
                row { dataButton("Я работал", "Я работал")  }
                row { dataButton("Я отдыхал", "Я отдыхал")  }
            }) {
            regular("Нужно оформить отчёт, для этого нажмите \"Я работал\"")
        }
    }
}