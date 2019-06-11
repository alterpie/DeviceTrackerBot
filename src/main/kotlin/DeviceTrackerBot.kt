import model.WhereIsCommandStatus
import org.apache.commons.lang3.text.StrBuilder
import org.telegram.telegrambots.bots.TelegramLongPollingBot
import org.telegram.telegrambots.meta.api.methods.send.SendMessage
import org.telegram.telegrambots.meta.api.objects.Message
import org.telegram.telegrambots.meta.api.objects.Update
import org.telegram.telegrambots.meta.exceptions.TelegramApiException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.random.Random


class DeviceTrackerBot : TelegramLongPollingBot() {
    override fun getBotUsername(): String = System.getenv("username")

    override fun getBotToken(): String = System.getenv("token")

    override fun onUpdateReceived(update: Update?) {
        update?.let {
            if (it.hasMessage() && it.message.hasText() && it.message.isCommand) {
                val parts = it.message.text.replace("/", "").split(" ")
                when {
                    COMMON_COMMANDS.any { command -> command == parts.first() } -> {
                        when (parts.first()) {
                            HELP -> helpMessage(it)
                            START -> helpMessage(it)
                        }
                    }
                    DEVICE_COMMANDS.any { command -> command == parts.first() } -> {
                        if (GoogleSheetsHelper.isUserAuthorized(it.message.from.id.toLong())) {
                            if (parts.size == 1) {
                                if (parts.first() == LIST) listDevicesMessage(it) else deviceIdNotSpecified(it)
                            } else {
                                val deviceId = parts[1]
                                when (parts.first()) {
                                    TAKE -> takeMessage(it, deviceId)
                                    RETURN -> returnMessage(it, deviceId)
                                    WHERE_IS -> whereIsMessage(it, deviceId)
                                }
                            }
                        } else {
                            notAuthorizedMessage(it)
                        }
                    }
                    else -> wrongInput(it)
                }
            } else {
                wrongInput(it)
            }
        }
    }

    private fun listDevicesMessage(update: Update) {
        val rows = GoogleSheetsHelper.readDeviceSheet().rows
        val sb = StrBuilder()
        rows.forEach {
            sb.append(it.manufacturer).append(", ").append(it.model).append(", ").append("S/N: ")
                .append(it.serialNumber).append(", ").append("OS: ")
                .append(it.osVersion).append(", ").append("<b>Device id: ${it.shortName}</b>").append(", ")
                .append("Owner: ").append(if (it.owner.isNotBlank()) it.owner else "Available").append("\n")
        }
        val message = createBaseMessage(update.message.chatId)
            .setText(sb.toString())
        sendMessage(message)
    }

    private fun notAuthorizedMessage(update: Update) {
        val message = createBaseMessage(update.message.chatId)
            .setText("You are not authorized to perform this action, sorry :(((\nTry to reach your supervisor for access")
        sendMessage(message)
    }

    private fun deviceIdNotSpecified(update: Update) {
        val message = createBaseMessage(update.message.chatId)
            .setText("Wrong command format, should be <b>command &lt;device_id&gt;</b>")
        sendMessage(message)
    }

    private fun wrongInput(update: Update) {
        val message = createBaseMessage(update.message.chatId)
            .setText("Wrong input, for list of all available commands submit /help")
        sendMessage(message)
    }

    private fun helpMessage(update: Update) {
        val message = createBaseMessage(update.message.chatId)
            .setText(
                "I can manage device whereabouts so every team member is aware where he/she can find specific device\n\n" +
                        "<b>Device</b>\n" +
                        "/take - associate your name with device\n" +
                        "/return - remove ownership of device\n" +
                        "/whereis - find device whereabouts\n" +
                        "/list - list of all available devices\n"
            )
        sendMessage(message)
    }

    private fun whereIsMessage(update: Update, deviceId: String) {
        val message = when (val status = GoogleSheetsHelper.whoHoldsDevice(deviceId)) {
            WhereIsCommandStatus.Free -> createBaseMessage(update.message.chatId).setText("Nobody holds <b>$deviceId</b>, it's on stand")
            WhereIsCommandStatus.NoSuchDevice -> createBaseMessage(update.message.chatId).setText("There is no such device <b>$deviceId</b>")
            is WhereIsCommandStatus.Taken -> createBaseMessage(update.message.chatId)
                .setText("${status.owner} holds <b>$deviceId</b>")
        }
        println("Where is message: $message")
        sendMessage(message)
    }

    private fun returnMessage(update: Update, deviceId: String) {
        val message = if (GoogleSheetsHelper.writeDeviceReturn(deviceId)) {
            createBaseMessage(update.message.chatId)
                .setText("You returned <b>$deviceId</b> to stand\n${randomAdditionalMessage(RETURN_ADDITIONAL_MESSAGES)}")
        } else {
            createBaseMessage(update.message.chatId)
                .setText("There is no such device <b>$deviceId</b>")
        }
        sendMessage(message)
    }

    private fun takeMessage(update: Update, deviceId: String) {
        val message = if (GoogleSheetsHelper.writeDeviceTake(update.username(), deviceId)) {
            createBaseMessage(update.message.chatId)
                .setText("You took <b>$deviceId</b> from stand\n${randomAdditionalMessage(TAKE_ADDITIONAL_MESSAGES)}")
        } else {
            createBaseMessage(update.message.chatId)
                .setText("There is no such device <b>$deviceId</b>")
        }

        sendMessage(message)
    }

    private fun sendMessage(sendMessage: SendMessage) {
        try {
            execute<Message, SendMessage>(sendMessage) // Sending our message object to user
        } catch (e: TelegramApiException) {
            e.printStackTrace()
        }
    }

    private fun createBaseMessage(chatId: Long) = SendMessage()
        .setChatId(chatId)
        .setParseMode("HTML")

    private fun Update.username() = "${message.from.firstName} ${message.from.lastName}".let {
        if (message.from.userName != null) {
            "$it (@${message.from.userName})"
        } else it
    }

    private fun randomAdditionalMessage(list: List<String>): String {
        val random = Random.nextInt(3)
        return list[random]
    }

    companion object {
        private const val TAKE = "take"
        private const val RETURN = "return"
        private const val WHERE_IS = "whereis"
        private const val HELP = "help"
        private const val START = "start"
        private const val LIST = "list"

        private val COMMON_COMMANDS = listOf(HELP, START)
        private val DEVICE_COMMANDS = listOf(TAKE, RETURN, WHERE_IS, LIST)

        @JvmStatic
        private val TAKE_ADDITIONAL_MESSAGES =
            listOf("Happy coding!", "Happy testing!", "Try to stay hydrated while working &lt;з")
        @JvmStatic
        private val RETURN_ADDITIONAL_MESSAGES = listOf(
            "Hope your coding was productive!",
            "This device's already missing you &lt;з",
            "Try to stay hydrated while working &lt;з"
        )

        private fun formatDate(date: Long) = SimpleDateFormat("dd-MM-yyyy HH:mm:ss").format(Date(date))
    }
}