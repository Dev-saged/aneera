package ps.anira.aid.ui.archive

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ps.anira.aid.data.IngestResult
import ps.anira.aid.data.MonthKey
import ps.anira.aid.data.OverallStats
import ps.anira.aid.data.Repository

data class ArchivedMonth(val key: String, val label: String, val count: Int)

sealed class ImportUiResult {
    data class Done(val result: IngestResult) : ImportUiResult()
    data class Failed(val message: String) : ImportUiResult()
}

class ArchiveViewModel(private val repo: Repository) : ViewModel() {

    private val _months = MutableStateFlow<List<ArchivedMonth>>(emptyList())
    val months: StateFlow<List<ArchivedMonth>> = _months.asStateFlow()

    private val _stats = MutableStateFlow(OverallStats(0, 0, 0, null))
    val stats: StateFlow<OverallStats> = _stats.asStateFlow()

    private val _importResult = MutableStateFlow<ImportUiResult?>(null)
    val importResult: StateFlow<ImportUiResult?> = _importResult.asStateFlow()

    init {
        viewModelScope.launch {
            repo.observeArchivedMonths(MonthKey.currentKey()).collect { keys ->
                _months.value = keys.map { key ->
                    ArchivedMonth(key = key, label = MonthKey.label(key), count = repo.countInMonth(key))
                }
                _stats.value = repo.overallStats() // يُعاد حسابها مع أي تغيّر بالأرشيف (وأيضاً بعد كل استيراد ناجح)
            }
        }
    }

    suspend fun buildExportJson(): String = repo.exportAllAsJson()

    fun importJson(content: String) {
        viewModelScope.launch {
            _importResult.value = try {
                val result = repo.importFromJson(content)
                _stats.value = repo.overallStats()
                ImportUiResult.Done(result)
            } catch (e: Exception) {
                ImportUiResult.Failed("الملف غير صالح أو تالف — تأكد أنه نسخة JSON مصدَّرة من أنيرا.")
            }
        }
    }

    fun consumeImportResult() { _importResult.value = null }
}
