package ps.anira.aid.backup

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.tgDataStore by preferencesDataStore(name = "anira_tg_settings")

data class TelegramConfig(val botToken: String = "", val chatId: String = "") {
    val isConfigured: Boolean get() = botToken.isNotBlank() && chatId.isNotBlank()
}

/**
 * يعادل getTgCfg()/setTgCfg() بالويب (localStorage['anira_tg_cfg']) — لا يُصدَّر
 * هذا التخزين أبداً ضمن نسخة JSON الاحتياطية (BackupBlob لا يحتوي على أي من
 * هذين الحقلين إطلاقاً)، تماماً كما بالويب.
 */
class TelegramSettingsStore(private val context: Context) {
    private val tokenKey = stringPreferencesKey("bot_token")
    private val chatIdKey = stringPreferencesKey("chat_id")

    val config: Flow<TelegramConfig> = context.tgDataStore.data.map { prefs ->
        TelegramConfig(
            botToken = prefs[tokenKey] ?: "",
            chatId = prefs[chatIdKey] ?: ""
        )
    }

    suspend fun save(botToken: String, chatId: String) {
        context.tgDataStore.edit { prefs ->
            prefs[tokenKey] = botToken.trim()
            prefs[chatIdKey] = chatId.trim()
        }
    }
}
