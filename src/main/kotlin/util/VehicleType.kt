package util

enum class VehicleType(val ru: String, val list: List<String>) {
    HEAVY("Спецтехника", heavyVehiclesList),
    LIGHT("Машина", lightVehiclesList),
}