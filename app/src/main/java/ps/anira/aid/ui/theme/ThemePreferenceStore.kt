package ps.anira.aid.ui.theme

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.themeDataStore by preferencesDataStore(name = "anira_theme_prefs")

/** يعادل localStorage('anira_theme') بالويب بالضبط. null = يتبع وضع النظام. */
class ThemePreferenceStore(private val context: Context) {
    private val darkKey = booleanPreferencesKey("dark_mode")
    private val hasPreferenceKey = booleanPreferencesKey("has_preference")

    /** null = لا تفضيل محفوظ (يتبع النظام)، true/false = تفضيل صريح محفوظ. */
    val isDarkFlow: Flow<Boolean?> = context.themeDataStore.data.map { prefs ->
        if (prefs[hasPreferenceKey] == true) prefs[darkKey] else null
    }

    suspend fun setDark(isDark: Boolean) {
        context.themeDataStore.edit { prefs ->
            prefs[darkKey] = isDark
            prefs[hasPreferenceKey] = true
        }
    }
}
