package model

import com.google.api.services.sheets.v4.model.ValueRange

data class UserSheet(val users: List<Long>) {

    companion object {
        fun parseToUserSheet(valueRange: ValueRange): UserSheet {
            var userSheet = UserSheet(emptyList())
            val values = valueRange.getValues()
            if (values != null) {
                val users = mutableListOf<Long>()
                values.forEach { row ->
                    row.forEach { cell ->
                        (cell as String).toLongOrNull()?.let {
                            users.add(it)
                        }
                    }
                }
                userSheet = userSheet.copy(users = users)
            }
            println("Parsed users: $userSheet")
            return userSheet
        }
    }
}