package ps.anira.aid.backup

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import ps.anira.aid.data.BeneficiaryRecord
import ps.anira.aid.data.MonthKey
import ps.anira.aid.data.RawIncomingRecord

/**
 * DTO مستقل تماماً عن كيان Room (BeneficiaryRecord) — يمثّل صيغة التبادل
 * الخارجي فقط (لا يحمل monthKey، وهو تفصيل تخزين داخلي بحت لا وجود له بصيغة
 * تصدير الويب). خلط الكيانين كان قراراً معمارياً خاطئاً صُحِّح هنا: أي إعادة
 * استخدام لكيان Room مباشرة بالتسلسل الخارجي يُسرّب تفاصيل تخزين داخلية
 * لصيغة يُفترض أن تبقى مستقرة ومتوافقة عبر النسختين (ويب/أصلي).
 */
@Serializable
data class BackupRecord(
    val id: String,
    val benName: String,
    val benId: String,
    val depName: String,
    val depId: String,
    val relation: String,
    val abroad: Boolean = false,
    val ts: Long
)

@Serializable
data class ArchiveMonth(
    val key: String,
    val label: String,
    val archivedAt: Long,
    val records: List<BackupRecord>
)

/**
 * نفس شكل تصدير JSON بالضبط الذي تنتجه buildBackupBlob() بالويب (app/v/exportedAt/
 * by/current/archives) — النسخة الأصلية الأندرويدية تبنيه من جدول مسطَّح واحد
 * داخلياً، لكن الشكل الخارجي مطابق تماماً، فملف النسخة الاحتياطية قابل
 * للاستيراد من/إلى نسخة الويب (WebToApp) بلا أي تعديل.
 */
@Serializable
data class BackupBlob(
    val app: String = "anira",
    val v: Int = 2,
    val exportedAt: String,
    val by: String = "Anira Android Native",
    val current: List<BackupRecord>,
    val archives: List<ArchiveMonth>
)

/** يسمح بقراءة ملفات مُصدَّرة من الويب حتى لو حملت حقولاً إضافية غير معروفة هنا مستقبلاً. */
val lenientBackupJson = Json { ignoreUnknownKeys = true }

fun BeneficiaryRecord.toBackupRecord() = BackupRecord(
    id = id, benName = benName, benId = benId, depName = depName,
    depId = depId, relation = relation, abroad = abroad, ts = ts
)

fun BackupRecord.toRawIncoming() = RawIncomingRecord(
    id = id, benName = benName, benId = benId, depName = depName,
    depId = depId, relation = relation, abroad = abroad, ts = ts
)

/** يسطّح current + كل سجلات archives لدفعة واحدة جاهزة لـ Repository.ingest(). */
fun BackupBlob.flattenToRaw(): List<RawIncomingRecord> =
    current.map { it.toRawIncoming() } + archives.flatMap { it.records.map { r -> r.toRawIncoming() } }
