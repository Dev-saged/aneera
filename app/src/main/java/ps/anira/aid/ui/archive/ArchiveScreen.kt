package ps.anira.aid.ui.archive

import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Today
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ps.anira.aid.data.MonthKey
import ps.anira.aid.data.OverallStats
import ps.anira.aid.data.Repository
import java.io.BufferedReader
import java.io.InputStreamReader

class ArchiveViewModelFactory(private val repo: Repository) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T = ArchiveViewModel(repo) as T
}

@Composable
fun ArchiveScreen(repo: Repository, onOpenMonth: (key: String, label: String) -> Unit, modifier: Modifier = Modifier) {
    val vm: ArchiveViewModel = viewModel(factory = ArchiveViewModelFactory(repo))
    val months by vm.months.collectAsState()
    val stats by vm.stats.collectAsState()
    val importResult by vm.importResult.collectAsState()
    val context = LocalContext.current
    val scope = androidx.compose.runtime.rememberCoroutineScope()
    var feedback by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val json = vm.buildExportJson()
            context.contentResolver.openOutputStream(uri)?.use { it.write(json.toByteArray(Charsets.UTF_8)) }
            feedback = "تم تصدير النسخة الكاملة بنجاح"
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        context.contentResolver.openInputStream(uri)?.use { stream ->
            val text = BufferedReader(InputStreamReader(stream, Charsets.UTF_8)).readText()
            vm.importJson(text)
        }
    }

    LaunchedEffect(importResult) {
        when (val r = importResult) {
            is ImportUiResult.Done -> feedback = "تم الدمج: ${r.result.added} أُضيف، ${r.result.duplicate} مكرَّر"
            is ImportUiResult.Failed -> feedback = r.message
            null -> {}
        }
        if (importResult != null) vm.consumeImportResult()
    }

    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text("الأرشيف", style = MaterialTheme.typography.titleLarge)

        StatsCard(stats)

        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("نسخة احتياطية كاملة (JSON)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(
                        onClick = { exportLauncher.launch("نسخة_أنيرا_${MonthKey.currentKey()}.json") },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FileDownload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  تصدير")
                    }
                    OutlinedButton(
                        onClick = { importLauncher.launch(arrayOf("application/json")) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Filled.FileUpload, contentDescription = null, modifier = Modifier.size(18.dp))
                        Text("  استيراد ودمج")
                    }
                }
                feedback?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }

        Spacer(Modifier.padding(top = 12.dp))

        Text("الأشهر المؤرشفة (${months.size})", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)

        if (months.isEmpty()) {
            Text(
                "لا يوجد أشهر مؤرشفة بعد. الأشهر تُضاف هنا تلقائياً بمجرّد أن يمرّ الشهر التقويمي الحالي.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(months, key = { it.key }) { month ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    onClick = { onOpenMonth(month.key, month.label) }
                ) {
                    Row(
                        Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(
                                Icons.Filled.Archive,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Column {
                                Text(month.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                Text(
                                    "${month.count} سجل",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(Icons.Filled.ChevronLeft, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

/** يعادل renderStats() بالويب: بطاقتا العدد الكلي/الشهر الحالي، ثم صف مصغّرات (خارج البلاد، أكثر صلة). */
@Composable
private fun StatsCard(stats: OverallStats) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile(icon = Icons.Filled.Archive, value = stats.total.toString(), label = "إجمالي الإنابات", modifier = Modifier.weight(1f))
                StatTile(icon = Icons.Filled.Today, value = stats.currentMonth.toString(), label = "الشهر الحالي", modifier = Modifier.weight(1f))
            }
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Icon(Icons.Filled.Public, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("خارج البلاد: ${stats.abroad}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (stats.topRelation != null) {
                    Text("أكثر صلة: ${stats.topRelation}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}

@Composable
private fun StatTile(icon: androidx.compose.ui.graphics.vector.ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
        Text(value, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
