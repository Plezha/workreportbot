package questions

import Answers
import dev.inmo.micro_utils.fsm.common.State
import dev.inmo.tgbotapi.types.ChatIdentifier
import dev.inmo.tgbotapi.types.IdChatIdentifier
import util.VehicleType

sealed interface BotState : State {
    override val context: IdChatIdentifier
}

data class DateQuestion(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val isEditing: Boolean = false
) : BotState {
    companion object {
        const val answerDescription = "Дата"
    }
}
data class VehicleQuestion(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val isEditing: Boolean = false
) : BotState {
    companion object {
        const val answerDescription = "Машина"
    }
}
data class HeavyVehicleEngineHoursQuestion(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val isEditing: Boolean = false
) : BotState {
    companion object {
        const val answerDescription = "Часов двигателя"
    }
}
data class LightVehicleMileageQuestion(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val isEditing: Boolean = false
) : BotState {
    companion object {
        const val answerDescription = "Пробег в км"
    }
}
data class HoursWorkedQuestion(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val isEditing: Boolean = false
) : BotState {
    companion object {
        const val answerDescription = "Часов отработано"
    }
}
data class ObjectDescriptionQuestion(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val isEditing: Boolean = false
) : BotState {
    companion object {
        const val answerDescription = "Описание объекта"
    }
}
data class TaskDescriptionQuestion(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val isEditing: Boolean = false
) : BotState {
    companion object {
        const val answerDescription = "Описание задания (кратко)"
    }
}
data class FuelUsedQuestion(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val isEditing: Boolean = false
) : BotState {
    companion object {
        const val answerDescription = "Топлива затрачено"
    }
}
data class FuelGotQuestion(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val isEditing: Boolean = false
) : BotState {
    companion object {
        const val answerDescription = "Топлива получено"
    }
}
data class MalfunctionQuestion(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val isEditing: Boolean = false
) : BotState {
    companion object {
        const val answerDescription = "Неисправности"
    }
}

data class SubmitState(
    override val context: IdChatIdentifier,
    val currentProgress: Answers,
    val chatId: ChatIdentifier
) : BotState

fun answerDescriptions(vehicleType: VehicleType) = listOf(
    DateQuestion.answerDescription,
    VehicleQuestion.answerDescription,
    if (vehicleType == VehicleType.HEAVY)
        HeavyVehicleEngineHoursQuestion.answerDescription
    else
        LightVehicleMileageQuestion.answerDescription,
    HoursWorkedQuestion.answerDescription,
    ObjectDescriptionQuestion.answerDescription,
    TaskDescriptionQuestion.answerDescription,
    FuelUsedQuestion.answerDescription,
    FuelGotQuestion.answerDescription,
    MalfunctionQuestion.answerDescription
)




