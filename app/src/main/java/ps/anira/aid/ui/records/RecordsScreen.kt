package ps.anira.aid.ui.records

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridOn
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import kotlinx.coroutines.launch
import ps.anira.aid.data.BeneficiaryRecord
import ps.anira.aid.data.Repository
import ps.anira.aid.export.PdfWriter
import ps.anira.aid.export.XlsxWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class RecordsViewModelFactory(
    private val repo: Repository, private val monthKey: String, private val monthLabel: String
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T =
        RecordsViewModel(repo, monthKey, monthLabel) as T
}

@Composable
fun RecordsScreen(
    repo: Repository,
    monthKey: String,
    monthLabel: String,
    modifier: Modifier = Modifier
) {
    val vm: RecordsViewModel = viewModel(factory = RecordsViewModelFactory(repo, monthKey, monthLabel))
    val state by vm.state.collectAsState()
    var editing by remember { mutableStateOf<BeneficiaryRecord?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var exportFeedback by remember { mutableStateOf<String?>(null) }

    val xlsxLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument(
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
        )
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val records = vm.allRecordsForExport()
            val bytes = XlsxWriter.build(records)
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            exportFeedback = "تم تصدير ${records.size} سجل إلى Excel بنجاح"
        }
    }

    val pdfLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val records = vm.allRecordsForExport()
            val bytes = PdfWriter.build(records, monthLabel)
            context.contentResolver.openOutputStream(uri)?.use { it.write(bytes) }
            exportFeedback = "تم تصدير ${records.size} سجل إلى PDF بنجاح"
        }
    }

    Column(modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Text("${state.monthLabel} · ${state.records.size} سجل", style = MaterialTheme.typography.titleMedium)
            Row {
                IconButton(onClick = {
                    xlsxLauncher.launch("سجلات_${state.monthLabel.replace(" ", "_")}.xlsx")
                }) {
                    Icon(Icons.Filled.GridOn, contentDescription = "تصدير Excel", tint = MaterialTheme.colorScheme.primary)
                }
                IconButton(onClick = {
                    pdfLauncher.launch("سجلات_${state.monthLabel.replace(" ", "_")}.pdf")
                }) {
                    Icon(Icons.Filled.PictureAsPdf, contentDescription = "تصدير PDF", tint = MaterialTheme.colorScheme.primary)
                }
            }
        }
        exportFeedback?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
        }

        OutlinedTextField(
            value = state.query,
            onValueChange = vm::onQueryChange,
            label = { Text("بحث بالاسم") },
            leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        SortSegmentedControl(current = state.sort, onSelect = vm::onSortChange)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(state.records, key = { it.id }) { rec ->
                RecordCard(
                    record = rec,
                    onEdit = { editing = rec },
                    onDelete = { vm.delete(rec) }
                )
            }
        }
    }

    editing?.let { rec ->
        EditRecordDialog(
            record = rec,
            repo = repo,
            onDismiss = { editing = null }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SortSegmentedControl(current: SortMode, onSelect: (SortMode) -> Unit) {
    val options = listOf(SortMode.NEWEST to "الأحدث", SortMode.OLDEST to "الأقدم", SortMode.NAME to "الاسم")
    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
        options.forEachIndexed { index, (mode, label) ->
            SegmentedButton(
                selected = current == mode,
                onClick = { onSelect(mode) },
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size)
            ) { Text(label) }
        }
    }
}

@Composable
private fun RecordCard(record: BeneficiaryRecord, onEdit: () -> Unit, onDelete: () -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(record.benName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Row {
                    IconButton(onClick = onEdit) { Icon(Icons.Filled.Edit, contentDescription = "تعديل", tint = MaterialTheme.colorScheme.primary) }
                    IconButton(onClick = onDelete) { Icon(Icons.Filled.Delete, contentDescription = "حذف", tint = MaterialTheme.colorScheme.error) }
                }
            }
            Text("المنيب: ${record.depName} · ${record.relation}", style = MaterialTheme.typography.bodyMedium)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(
                    SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.US).format(Date(record.ts)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (record.abroad) {
                    Icon(Icons.Filled.Public, contentDescription = "خارج البلاد", modifier = Modifier.padding(start = 4.dp), tint = MaterialTheme.colorScheme.primary)
                    Text("خارج البلاد", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                }
            }
        }
    }
}

@Composable
private fun EditRecordDialog(record: BeneficiaryRecord, repo: Repository, onDismiss: () -> Unit) {
    var benName by remember { mutableStateOf(record.benName) }
    var benId by remember { mutableStateOf(record.benId) }
    var depName by remember { mutableStateOf(record.depName) }
    var depId by remember { mutableStateOf(record.depId) }
    var relation by remember { mutableStateOf(record.relation) }
    var abroad by remember { mutableStateOf(record.abroad) }
    val scope = androidx.compose.runtime.rememberCoroutineScope()

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("تعديل الإنابة") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = benName, onValueChange = { benName = it }, label = { Text("اسم المستفيد") }, singleLine = true)
                OutlinedTextField(
                    value = benId,
                    onValueChange = { benId = it.filter(Char::isDigit).take(9) },
                    label = { Text("هوية المستفيد") }, singleLine = true
                )
                OutlinedTextField(value = depName, onValueChange = { depName = it }, label = { Text("اسم المنيب") }, singleLine = true)
                OutlinedTextField(
                    value = depId,
                    onValueChange = { depId = it.filter(Char::isDigit).take(9) },
                    label = { Text("هوية المنيب") }, singleLine = true
                )
                OutlinedTextField(value = relation, onValueChange = { relation = it }, label = { Text("صلة القرابة") }, singleLine = true)
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    repo.update(record, benName, benId, depName, depId, relation, abroad)
                    onDismiss()
                }
            }) { Text("حفظ") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("إلغاء") } }
    )
}
