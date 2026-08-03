package ps.anira.aid.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ps.anira.aid.backup.TelegramClient
import ps.anira.aid.backup.TelegramSendResult
import ps.anira.aid.backup.TelegramSettingsStore
import ps.anira.aid.data.MonthKey
import ps.anira.aid.data.Repository

enum class SendStatus { IDLE, SENDING, SUCCESS, ERROR }

data class SettingsUiState(
    val botToken: String = "",
    val chatId: String = "",
    val sendStatus: SendStatus = SendStatus.IDLE,
    val errorMessage: String? = null
)

class SettingsViewModel(
    private val repo: Repository,
    private val store: TelegramSettingsStore,
    private val client: TelegramClient = TelegramClient()
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsUiState())
    val state: StateFlow<SettingsUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            store.config.collect { cfg ->
                _state.value = _state.value.copy(botToken = cfg.botToken, chatId = cfg.chatId)
            }
        }
    }

    fun onTokenChange(v: String) { _state.value = _state.value.copy(botToken = v) }
    fun onChatIdChange(v: String) { _state.value = _state.value.copy(chatId = v) }

    private suspend fun saveConfigSuspend() {
        store.save(_state.value.botToken, _state.value.chatId)
    }

    /** استخدام مباشر من الواجهة (زر "حفظ") بلا حاجة لانتظار صريح. */
    fun saveConfig() {
        viewModelScope.launch { saveConfigSuspend() }
    }

    /** يعادل sendBackupToTelegram() بالويب: يبني نسخة JSON الكاملة ويرفعها. */
    fun sendBackupNow() {
        val s = _state.value
        if (s.botToken.isBlank() || s.chatId.isBlank()) {
            _state.value = s.copy(errorMessage = "أدخل التوكن ومعرّف المحادثة أولاً.")
            return
        }
        _state.value = s.copy(sendStatus = SendStatus.SENDING, errorMessage = null)
        viewModelScope.launch {
            saveConfigSuspend() // ينتظر فعلياً قبل المتابعة (لا إطلاق منفصل غير متزامن)
            val json = repo.exportAllAsJson()
            val fileName = "نسخة_أنيرا_${MonthKey.currentKey()}.json"
            when (val result = client.sendDocument(s.botToken, s.chatId, fileName, json, "نسخة احتياطية — أنيرا")) {
                is TelegramSendResult.Success ->
                    _state.value = _state.value.copy(sendStatus = SendStatus.SUCCESS, errorMessage = null)
                is TelegramSendResult.Failure ->
                    _state.value = _state.value.copy(sendStatus = SendStatus.ERROR, errorMessage = result.message)
            }
        }
    }

    fun consumeStatus() {
        _state.value = _state.value.copy(sendStatus = SendStatus.IDLE, errorMessage = null)
    }
}
