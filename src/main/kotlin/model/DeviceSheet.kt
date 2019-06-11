package model

import com.google.api.services.sheets.v4.model.ValueRange

data class DeviceSheet(val rows: List<DeviceRow>) {

    fun getRowCount() = rows.size

    fun asRowList(): List<List<Any>> {
        val resultList = mutableListOf<List<Any>>()
        rows.forEach { row ->
            resultList.add(
                listOf(
                    row.manufacturer,
                    row.model,
                    row.serialNumber,
                    row.osVersion,
                    row.shortName,
                    row.owner
                )
            )
        }
        println(resultList.toString())
        return resultList
    }

    companion object {
        fun parseToDeviceSheet(valueRange: ValueRange): DeviceSheet {
            var deviceSheet = DeviceSheet(emptyList())
            val values = valueRange.getValues()
            if (values != null) {
                val rows = mutableListOf<DeviceRow>()
                values.forEach { row ->
                    rows.add(
                        DeviceRow(
                            row[0] as String,
                            row[1] as String,
                            row[2] as String,
                            row[3] as String,
                            row[4] as String,
                            row.getOrElse(5) { "" } as String
                        )
                    )
                }
                deviceSheet = deviceSheet.copy(rows = rows)
            }
            println("Parsed device sheet: $deviceSheet")
            return deviceSheet
        }
    }
}