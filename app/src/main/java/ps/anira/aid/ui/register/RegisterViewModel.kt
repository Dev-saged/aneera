package ps.anira.aid.ui.register

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ps.anira.aid.data.BeneficiaryRecord
import ps.anira.aid.data.IdChecksum
import ps.anira.aid.data.Repository
import ps.anira.aid.ui.RelationOptions

/** حالة تلميح واحد (تكرار أو checksum) لحقل هوية معيّن — يعادل بنية .dup-hint/.chk-hint بالويب. */
data class IdHintState(
    val duplicateText: String? = null, // null = لا تكرار، غير null = نص التلميح
    val checksumWarn: Boolean = false
)

data class RegisterUiState(
    val benName: String = "",
    val benId: String = "",
    val depName: String = "",
    val depId: String = "",
    val relation: String = "",
    val customRelation: String = "",
    val abroad: Boolean = false,
    val benHint: IdHintState = IdHintState(),
    val depHint: IdHintState = IdHintState(),
    val isSaving: Boolean = false,
    val hasPreviousRecord: Boolean = false, // يتحكم بظهور زر "نفس آخر منيب"
    val fieldError: String? = null,
    val lastSavedOk: Boolean = false
)

class RegisterViewModel(private val repo: Repository) : ViewModel() {

    private val _state = MutableStateFlow(RegisterUiState())
    val state: StateFlow<RegisterUiState> = _state.asStateFlow()

    private var lastSavedRecord: BeneficiaryRecord? = null

    init {
        refreshHasPrevious()
    }

    // نسخة داخلية من "seen batch" الخاصة بالحقل الحالي فقط لغرض debounce كل حقل على حدة
    private var benIdJob: kotlinx.coroutines.Job? = null
    private var depIdJob: kotlinx.coroutines.Job? = null

    fun onBenNameChange(v: String) { _state.value = _state.value.copy(benName = v) }
    fun onDepNameChange(v: String) { _state.value = _state.value.copy(depName = v) }
    fun onRelationChange(v: String) { _state.value = _state.value.copy(relation = v) }
    fun onCustomRelationChange(v: String) { _state.value = _state.value.copy(customRelation = v) }
    fun onAbroadToggle() { _state.value = _state.value.copy(abroad = !_state.value.abroad) }

    /** يعادل استماع 'input' على benId/depId بالويب: تنقية أرقام فقط + قصّ لـ9 + فحص تكرار/checksum بعد فاصل قصير. */
    fun onBenIdChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(9)
        _state.value = _state.value.copy(benId = digits)
        benIdJob?.cancel()
        benIdJob = viewModelScope.launch {
            delay(150) // يعادل الـdebounce بالويب (150ms)
            refreshHint(digits, isBen = true)
        }
    }

    fun onDepIdChange(raw: String) {
        val digits = raw.filter { it.isDigit() }.take(9)
        _state.value = _state.value.copy(depId = digits)
        depIdJob?.cancel()
        depIdJob = viewModelScope.launch {
            delay(150)
            refreshHint(digits, isBen = false)
        }
    }

    private suspend fun refreshHint(id: String, isBen: Boolean) {
        val checksumWarn = id.length == 9 && !IdChecksum.isOk(id)
        val dupText: String? = if (id.length == 9) {
            val hits = repo.findDuplicates(id)
            if (hits.isEmpty()) null else {
                val names = hits.map { if (it.benId == id) it.benName else it.depName }.distinct()
                "مكرّر: ${names.firstOrNull() ?: ""} — ${hits.size} مرة"
            }
        } else null
        val hint = IdHintState(duplicateText = dupText, checksumWarn = checksumWarn)
        _state.value = if (isBen) _state.value.copy(benHint = hint) else _state.value.copy(depHint = hint)
    }

    /** يعادل fillLastDep() بالويب — يقرأ من القاعدة فعلياً، لا من متغيّر جلسة فقط (يعمل حتى بعد إعادة فتح التطبيق). */
    fun fillFromLastDep() {
        viewModelScope.launch {
            val last = lastSavedRecord ?: repo.findMostRecent() ?: return@launch
            onDepNameChange(last.depName)
            onDepIdChange(last.depId)
        }
    }

    fun refreshHasPrevious() {
        viewModelScope.launch {
            _state.value = _state.value.copy(hasPreviousRecord = repo.hasAnyRecords())
        }
    }

    /** يعادل saveData() بالويب حرفياً — بما فيه قفل الضغط المزدوج. */
    fun save(onSaved: () -> Unit) {
        val s = _state.value
        if (s.isSaving) return // يعادل _saving guard بالويب — أي نقرة ثانية أثناء الحفظ تُتجاهل فوراً

        val benName = s.benName.trim()
        val depName = s.depName.trim()
        if (benName.isEmpty() || s.benId.length != 9 || depName.isEmpty() || s.depId.length != 9) {
            _state.value = s.copy(fieldError = "يرجى تعبئة جميع الحقول (رقم الهوية 9 أرقام).")
            return
        }
        var relation = s.relation
        if (relation.isEmpty()) {
            _state.value = s.copy(fieldError = "يرجى اختيار صلة القرابة.")
            return
        }
        if (relation == RelationOptions.CUSTOM) {
            relation = s.customRelation.trim()
            if (relation.isEmpty()) {
                _state.value = s.copy(fieldError = "اكتب صلة القرابة.")
                return
            }
        }

        _state.value = s.copy(isSaving = true, fieldError = null)
        viewModelScope.launch {
            try {
                val record = repo.insertNew(
                    benName = benName, benId = s.benId,
                    depName = depName, depId = s.depId,
                    relation = relation, abroad = s.abroad
                )
                lastSavedRecord = record
                _state.value = RegisterUiState(hasPreviousRecord = true, lastSavedOk = true)
                onSaved()
            } finally {
                if (_state.value.isSaving) _state.value = _state.value.copy(isSaving = false)
            }
        }
    }

    fun consumeSavedFlag() {
        _state.value = _state.value.copy(lastSavedOk = false)
    }

    fun consumeError() {
        _state.value = _state.value.copy(fieldError = null)
    }
}
