package storage

import com.google.gson.GsonBuilder
import dev.inmo.tgbotapi.types.ChatId
import dev.inmo.tgbotapi.types.RawChatId
import util.toLocalDateTime
import java.io.*
import java.time.LocalDateTime


data class User(val id: String, val lastReportStarted: Long, val lastReportSubmitted: Long) {
    val chatId
        get() = ChatId(RawChatId(id.toLong()))
    val lastReportStartedLocalDateTime: LocalDateTime
        get() = lastReportStarted.toLocalDateTime()
    val lastReportSubmittedLocalDateTime: LocalDateTime
        get() = lastReportSubmitted.toLocalDateTime()
}

class UserStorage {
    private val gson = GsonBuilder().setPrettyPrinting().create()
    private val file = File("users.json")

    private fun loadUsersFromFile(): MutableList<User> {
        if (!file.exists()) return mutableListOf()

        val json = file.readText()
        return gson.fromJson(json, Array<User>::class.java).toMutableList()
    }

    private fun saveUsersToFile(users: List<User>) {
        val json = gson.toJson(users.toTypedArray())
        file.writeText(json)
    }

    fun getAllUsers(): List<User> {
        return loadUsersFromFile()
    }

    fun getAllUsersAsMap(): Map<String, User> {
        return loadUsersFromFile().associateBy(User::id)
    }

    fun addUser(user: User) {
        val users = loadUsersFromFile()
        users.add(user)
        saveUsersToFile(users)
    }

    fun editUser(user: User) {
        val users = loadUsersFromFile()
        val index = users.indexOfFirst { it.id == user.id }
        if (index != -1) {
            users[index] = user
            saveUsersToFile(users)
        } else {
            throw IllegalArgumentException("No user to edit")
        }
    }

    fun removeUser(id: String) {
        val users = loadUsersFromFile()
        val index = users.indexOfFirst { it.id == id }
        if (index != -1) {
            users.removeAt(index)
            saveUsersToFile(users)
        }
    }
}