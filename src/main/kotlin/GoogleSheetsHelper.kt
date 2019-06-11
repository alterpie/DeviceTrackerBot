import com.google.api.client.auth.oauth2.Credential
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.ClearValuesRequest
import com.google.api.services.sheets.v4.model.ValueRange
import model.DeviceSheet
import model.UserSheet
import model.WhereIsCommandStatus
import org.json.JSONObject
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.util.Collections.singletonList


class GoogleSheetsHelper {

    companion object {
        private const val APPLICATION_NAME = "CyberDeviceTracker"
        private const val CREDENTIALS_FILE_PATH = "/client_secret.json"
        @JvmStatic
        private val SCOPES = singletonList(SheetsScopes.SPREADSHEETS)
        private const val DEVICE_SHEET_RANGE = "Sheet1"
        private const val USERS_SHEET_RANGE = "Sheet2"
        @JvmStatic
        private var credentials: Credential? = null

        private fun getCredentials(): Credential {
            if (credentials == null) {
                val credentialsFile = GoogleSheetsHelper::class.java.getResourceAsStream(CREDENTIALS_FILE_PATH)
                credentials = GoogleCredential.fromStream(credentialsFile)
                    .createScoped(SCOPES)
            }
            return credentials!!
        }

        private fun getSpreadsheet(): Sheets = Sheets.Builder(
            GoogleNetHttpTransport.newTrustedTransport(),
            JacksonFactory.getDefaultInstance(),
            getCredentials()
        ).setApplicationName(APPLICATION_NAME).build()

        fun writeDeviceTake(username: String, deviceId: String): Boolean {
            val deviceSheet = readDeviceSheet()

            if (deviceSheet.rows.any { it.shortName.equals(deviceId, true) }) {
                val updatedDeviceSheet = deviceSheet.copy(rows = deviceSheet.rows.toMutableList().apply {
                    val indexOfRow = indexOfFirst { it.shortName.equals(deviceId, true) }
                    if (indexOfRow != -1) {
                        val oldRow = get(indexOfRow)
                        removeAt(indexOfRow)
                        add(indexOfRow, oldRow.copy(owner = username))
                    }
                })

                val values = updatedDeviceSheet.asRowList()

                println("Writing values $values")

                clearSheet(DEVICE_SHEET_RANGE)

                val body = ValueRange()
                    .setValues(values)
                getSpreadsheet().spreadsheets().values()
                    .update(System.getenv("spreadsheetId"), DEVICE_SHEET_RANGE, body)
                    .setValueInputOption("RAW")
                    .execute()
                return true
            } else {
                return false
            }
        }

        fun writeDeviceReturn(deviceId: String): Boolean {
            val deviceSheet = readDeviceSheet()

            if (deviceSheet.rows.any { it.shortName == deviceId }) {
                val updatedDeviceSheet = deviceSheet.copy(rows = deviceSheet.rows.toMutableList().apply {
                    val indexOfRow = indexOfFirst { it.shortName == deviceId }
                    if (indexOfRow != -1) {
                        val oldRow = get(indexOfRow)
                        removeAt(indexOfRow)
                        add(indexOfRow, oldRow.copy(owner = ""))
                    }
                })

                val values = updatedDeviceSheet.asRowList()

                println("Writing values $values")

                clearSheet(DEVICE_SHEET_RANGE)

                val body = ValueRange()
                    .setValues(values)
                getSpreadsheet().spreadsheets().values()
                    .update(System.getenv("spreadsheetId"), DEVICE_SHEET_RANGE, body)
                    .setValueInputOption("RAW")
                    .execute()
                return true
            } else {
                return false
            }
        }

        fun whoHoldsDevice(deviceId: String): WhereIsCommandStatus {
            val deviceSheet = readDeviceSheet()

            val indexOfDeviceRow = deviceSheet.rows.indexOfFirst { it.shortName.equals(deviceId, true) }

            return if (indexOfDeviceRow != -1) {
                val owner = deviceSheet.rows[indexOfDeviceRow].owner
                if (owner.isBlank()) WhereIsCommandStatus.Free else {
                    WhereIsCommandStatus.Taken(owner)
                }
            } else WhereIsCommandStatus.NoSuchDevice
        }

        fun isUserAuthorized(userId: Long): Boolean {
            val userSheet = readUserSheet()
            return userSheet.users.contains(userId)
        }

        private fun clearSheet(sheetName: String) {
            getSpreadsheet().spreadsheets().values()
                .clear(System.getenv("spreadsheetId"), sheetName, ClearValuesRequest())
                .execute()
        }

        fun readDeviceSheet(): DeviceSheet {
            return DeviceSheet.parseToDeviceSheet(
                getSpreadsheet().spreadsheets().values().get(
                    System.getenv("spreadsheetId"),
                    DEVICE_SHEET_RANGE
                ).execute()
            )
        }

        private fun readUserSheet(): UserSheet {
            return UserSheet.parseToUserSheet(
                getSpreadsheet().spreadsheets().values().get(
                    System.getenv("spreadsheetId"),
                    USERS_SHEET_RANGE
                ).execute()
            )
        }
    }
}