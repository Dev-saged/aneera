package ps.anira.aid

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ps.anira.aid.data.AniraDatabase
import ps.anira.aid.data.MonthKey
import ps.anira.aid.ui.theme.AniraTheme

/**
 * هذا الأساس (foundation) يثبت أن السلسلة كاملة تعمل من طرف لطرف: Gradle يبني،
 * Compose يرسم، Room يقرأ فعلياً من قاعدة بيانات حقيقية بمساحة التطبيق.
 * الشاشة الكاملة (نموذج التسجيل، السجلات، الأرشيف، الإعدادات...) تُبنى تباعاً
 * بمراحل لاحقة تُختبر كل واحدة بشكل مستقل قبل الانتقال للتي تليها — نفس فلسفة
 * "لا تراكم أخطاء" المعتمدة بمشروع الويب.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val db = AniraDatabase.get(applicationContext)

        setContent {
            AniraTheme {
                Scaffold { padding ->
                    val currentCount by db.recordDao()
                        .observeCurrentCount(MonthKey.currentKey())
                        .collectAsState(initial = 0)

                    FoundationScreen(
                        modifier = Modifier.fillMaxSize().padding(padding),
                        currentMonthLabel = MonthKey.label(MonthKey.currentKey()),
                        currentCount = currentCount
                    )
                }
            }
        }
    }
}

@Composable
private fun FoundationScreen(
    modifier: Modifier = Modifier,
    currentMonthLabel: String,
    currentCount: Int
) {
    Column(
        modifier = modifier.padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("أنيرا — أساس Native", style = MaterialTheme.typography.titleLarge)
        Text("سجلّ شهر: $currentMonthLabel", style = MaterialTheme.typography.bodyMedium)
        Text("عدد سجلات الشهر الحالي: $currentCount", style = MaterialTheme.typography.bodyLarge)
        Text(
            "هذه شاشة تأسيسية تثبت أن Gradle وCompose وRoom يعملون معاً بنجاح. " +
                "نموذج التسجيل والشاشات الكاملة تُبنى بالمرحلة التالية.",
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
