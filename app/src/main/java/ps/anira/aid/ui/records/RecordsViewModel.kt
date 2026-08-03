package ps.anira.aid.ui.records

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import ps.anira.aid.data.BeneficiaryRecord
import ps.anira.aid.data.MonthKey
import ps.anira.aid.data.Repository

enum class SortMode { NEWEST, OLDEST, NAME }

data class RecordsUiState(
    val query: String = "",
    val sort: SortMode = SortMode.NEWEST,
    val records: List<BeneficiaryRecord> = emptyList(),
    val monthLabel: String = ""
)

/**
 * monthKey = null → الشهر الحالي (الافتراضي، يعادل شاشة "السجلات" بالويب).
 * monthKey != null → عرض شهر مؤرشف محدَّد (يُعاد استخدام نفس الـViewModel لشاشة
 * تفاصيل الشهر بالأرشيف، لتفادي ازدواجية منطق الفرز/البحث).
 */
class RecordsViewModel(
    private val repo: Repository,
    private val monthKey: String,
    val monthLabel: String
) : ViewModel() {

    private val query = MutableStateFlow("")
    private val sort = MutableStateFlow(SortMode.NEWEST)
    private val records = repo.observeByMonth(monthKey)

    val state: StateFlow<RecordsUiState> = combine(records, query, sort) { list, q, s ->
        val filtered = if (q.isBlank()) list else list.filter {
            it.benName.contains(q, ignoreCase = true) || it.depName.contains(q, ignoreCase = true)
        }
        val sorted = when (s) {
            SortMode.NEWEST -> filtered.sortedByDescending { it.ts }
            SortMode.OLDEST -> filtered.sortedBy { it.ts }
            SortMode.NAME -> filtered.sortedBy { it.benName }
        }
        RecordsUiState(query = q, sort = s, records = sorted, monthLabel = monthLabel)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), RecordsUiState(monthLabel = monthLabel))

    fun onQueryChange(v: String) { query.value = v }
    fun onSortChange(v: SortMode) { sort.value = v }

    /** للتصدير: كل سجلات الشهر بلا فلترة بحث (البحث أداة عرض فقط، لا يُقصي بيانات من التصدير). */
    suspend fun allRecordsForExport(): List<BeneficiaryRecord> = repo.observeByMonthSyncSnapshot(monthKey)

    fun delete(record: BeneficiaryRecord) {
        viewModelScope.launch { repo.delete(record) }
    }

    companion object {
        fun currentMonth() = MonthKey.currentKey()
    }
}
