package ps.anira.aid.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    background = AniraLight.bg,
    surface = AniraLight.card,
    surfaceVariant = AniraLight.card2,
    onBackground = AniraLight.fg,
    onSurface = AniraLight.fg,
    primary = AniraLight.ink,
    secondary = AniraLight.gold,
    error = AniraLight.red,
    outline = AniraLight.line,
)

private val DarkColors = darkColorScheme(
    background = AniraDark.bg,
    surface = AniraDark.card,
    surfaceVariant = AniraDark.card2,
    onBackground = AniraDark.fg,
    onSurface = AniraDark.fg,
    primary = AniraDark.fg,
    secondary = AniraDark.gold,
    error = AniraDark.red,
    outline = AniraDark.line,
)

/**
 * لا يوجد زر تبديل يدوي بعد بهذا الأساس (foundation) — يتبع فقط إعداد النظام
 * (isSystemInDarkTheme). عند بناء شاشة الإعدادات لاحقاً يمكن ربطها بتفضيل محفوظ
 * بنفس منطق زر الوضع الليلي بالنسخة الويب (localStorage → DataStore).
 */
@Composable
fun AniraTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColors else LightColors
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AniraTypography,
        content = content
    )
}
