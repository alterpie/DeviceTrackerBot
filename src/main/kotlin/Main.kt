import org.telegram.telegrambots.ApiContextInitializer
import org.telegram.telegrambots.meta.TelegramBotsApi
import org.telegram.telegrambots.meta.exceptions.TelegramApiException

fun main() {
    ApiContextInitializer.init()

    val botsApi = TelegramBotsApi()

    try {
        botsApi.registerBot(DeviceTrackerBot())
    } catch (e: TelegramApiException) {
        e.printStackTrace()
    }
}