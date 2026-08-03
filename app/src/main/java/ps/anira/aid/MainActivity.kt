package ps.anira.aid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import ps.anira.aid.backup.TelegramSettingsStore
import ps.anira.aid.data.AniraDatabase
import ps.anira.aid.data.Repository
import ps.anira.aid.ui.nav.AniraApp
import ps.anira.aid.ui.theme.AniraTheme
import ps.anira.aid.ui.theme.ThemePreferenceStore

/**
 * v0.6.0: الوضع الليلي مضاف — تفضيل صريح محفوظ (يتفوّق على وضع النظام)،
 * يعادل زر القمر/الشمس بهيدر الويب بالضبط، بما فيه منع وميض عند الفتح
 * (نقرأ التفضيل قبل أول رسم عبر collectAsState بقيمة ابتدائية null → نظام).
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AniraDatabase.get(applicationContext)
        val repo = Repository(db.recordDao())
        val tgStore = TelegramSettingsStore(applicationContext)
        val themeStore = ThemePreferenceStore(applicationContext)

        setContent {
            val explicitDark by themeStore.isDarkFlow.collectAsState(initial = null)
            val isDark = explicitDark ?: isSystemInDarkTheme()

            AniraTheme(darkTheme = isDark) {
                AniraApp(
                    repo = repo,
                    tgStore = tgStore,
                    themeStore = themeStore,
                    isDark = isDark
                )
            }
        }
    }
}
