package model

data class DeviceRow(
    val manufacturer: String,
    val model: String,
    val serialNumber: String,
    val osVersion: String,
    val shortName: String,
    val owner: String
)