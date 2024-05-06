import questions.*
import util.VehicleType

data class Answers(val user: String) {
    var date: String? = null
    var vehicle: String? = null
    var hoursWorked: String? = null
    var vehicleType: VehicleType? = null
    var engineHours: String? = null
    var mileage: String? = null
    var objectDescription: String? = null
    var taskDescription: String? = null
    var fuelUsed: String? = null
    var fuelGot: String? = null
    var malfunctions: String? = null

    fun toReportList(): List<String?> = listOf(
        user,
        date,
        vehicle,
        hoursWorked,
        engineHours,
        mileage,
        objectDescription,
        taskDescription,
        fuelUsed,
        fuelGot,
        malfunctions
    ).map { it ?: "" }

    override fun toString(): String =
        """ |${DateQuestion.answerDescription}: $date
            |${VehicleQuestion.answerDescription}: $vehicle
            |${
            if (vehicleType == VehicleType.HEAVY)
                "${HeavyVehicleEngineHoursQuestion.answerDescription}: $engineHours"
            else
                "${LightVehicleMileageQuestion.answerDescription}: $mileage"
            }
            |${HoursWorkedQuestion.answerDescription}: $hoursWorked
            |${ObjectDescriptionQuestion.answerDescription}: $objectDescription
            |${TaskDescriptionQuestion.answerDescription}: $taskDescription
            |${FuelUsedQuestion.answerDescription}: $fuelUsed
            |${FuelGotQuestion.answerDescription}: $fuelGot
            |${MalfunctionQuestion.answerDescription}: ${malfunctions ?: "Неисправностей нет"}""".trimMargin()
}
