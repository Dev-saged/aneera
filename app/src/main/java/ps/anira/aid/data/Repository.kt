package ps.anira.aid.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import ps.anira.aid.backup.ArchiveMonth
import ps.anira.aid.backup.BackupBlob
import ps.anira.aid.backup.flattenToRaw
import ps.anira.aid.backup.lenientBackupJson
import ps.anira.aid.backup.toBackupRecord
import java.time.Instant

data class IngestResult(val added: Int, val duplicate: Int)

data class OverallStats(val total: Int, val currentMonth: Int, val abroad: Int, val topRelation: String?)

/** سجل خام قادم من استيراد JSON/Excel — قبل التطبيع، يعادل الكائن الممرَّر لـ normalizeRec() بالويب. */
data class RawIncomingRecord(
    val id: String?,
    val benName: String,
    val benId: String,
    val depName: String,
    val depId: String,
    val relation: String,
    val abroad: Boolean,
    val ts: Long?
)

class Repository(private val dao: RecordDao) {

    /** يعادل saveData() بالويب — إدخال سجل جديد واحد من نموذج التسجيل. */
    suspend fun insertNew(
        benName: String, benId: String, depName: String, depId: String,
        relation: String, abroad: Boolean, ts: Long = System.currentTimeMillis()
    ): BeneficiaryRecord = withContext(Dispatchers.IO) {
        val record = BeneficiaryRecord(
            id = IdGen.generate(),
            benName = benName.trim(),
            benId = onlyDigits(benId),
            depName = depName.trim(),
            depId = onlyDigits(depId),
            relation = relation.trim().ifBlank { "غير محدد" },
            abroad = abroad,
            ts = ts,
            monthKey = MonthKey.of(ts)
        )
        dao.insert(record)
        record
    }

    /**
     * يعادل ingestRecords() بالويب حرفياً (المُصلَحة بـ v1.2.0):
     * - التوجيه حسب monthKey المشتق من توقيت السجل نفسه، لا حسب وجود أرشيف محلي
     *   من عدمه — هذا بالضبط ما يمنع باگ "سجلات مؤرشفة تُحقن بالحالي عند نقلها
     *   لجهاز لم يؤرشف ذلك الشهر محلياً بعد".
     * - منع التكرار عبر مجموعة seen مبنية دفعة واحدة (استعلام واحد لكل الدفعة،
     *   لا استعلام لكل سجل) — نفس أداء نسخة الويب تماماً.
     * - كل id يمرّ عبر IdGen.safe قبل الإدراج (يعادل safeId() بالويب).
     */
    suspend fun ingest(raw: List<RawIncomingRecord>): IngestResult = withContext(Dispatchers.IO) {
        val seen = dao.getAllIds().toHashSet()
        var added = 0
        var duplicate = 0
        val toInsert = mutableListOf<BeneficiaryRecord>()

        for (r in raw) {
            val safeId = IdGen.safe(r.id)
            if (seen.contains(safeId)) {
                duplicate++
                continue
            }
            seen.add(safeId)
            val ts = r.ts ?: System.currentTimeMillis()
            toInsert.add(
                BeneficiaryRecord(
                    id = safeId,
                    benName = r.benName.trim(),
                    benId = onlyDigits(r.benId),
                    depName = r.depName.trim(),
                    depId = onlyDigits(r.depId),
                    relation = r.relation.trim().ifBlank { "غير محدد" },
                    abroad = r.abroad,
                    ts = ts,
                    monthKey = MonthKey.of(ts)
                )
            )
            added++
        }

        if (toInsert.isNotEmpty()) dao.insertAll(toInsert)
        IngestResult(added, duplicate)
    }

    suspend fun delete(record: BeneficiaryRecord) = withContext(Dispatchers.IO) { dao.delete(record) }

    /**
     * يعادل editRecord() بالويب — يحدّث سجلاً موجوداً بمعرّفه الأصلي (id يبقى ثابتاً).
     * monthKey يُعاد حسابه من ts الجديد (لو المستخدم عدّل توقيتاً يدوياً مستقبلاً؛
     * حالياً ts ثابت عند التعديل، تماماً كسلوك editRecord بالويب).
     */
    suspend fun update(
        original: BeneficiaryRecord,
        benName: String, benId: String, depName: String, depId: String,
        relation: String, abroad: Boolean
    ) = withContext(Dispatchers.IO) {
        val updated = original.copy(
            benName = benName.trim(),
            benId = onlyDigits(benId),
            depName = depName.trim(),
            depId = onlyDigits(depId),
            relation = relation.trim().ifBlank { "غير محدد" },
            abroad = abroad
        )
        dao.update(updated)
    }

    suspend fun countInMonth(monthKey: String): Int = withContext(Dispatchers.IO) { dao.countInMonth(monthKey) }

    /** يعادل renderStats() بالويب (البطاقات + أكثر صلة تكراراً) — يُستخدم بشاشة الأرشيف. */
    suspend fun overallStats(): OverallStats = withContext(Dispatchers.IO) {
        OverallStats(
            total = dao.totalCount(),
            currentMonth = dao.countInMonth(MonthKey.currentKey()),
            abroad = dao.abroadCount(),
            topRelation = dao.topRelation()
        )
    }

    fun observeByMonth(monthKey: String) = dao.observeByMonth(monthKey)

    suspend fun observeByMonthSyncSnapshot(monthKey: String): List<BeneficiaryRecord> =
        withContext(Dispatchers.IO) { dao.observeByMonthSync(monthKey) }

    fun observeArchivedMonths(currentMonthKey: String) = dao.observeArchivedMonths(currentMonthKey)

    fun observeCurrentCount(currentMonthKey: String) = dao.observeCurrentCount(currentMonthKey)

    suspend fun hasAnyRecords(): Boolean = withContext(Dispatchers.IO) { dao.getAllIds().isNotEmpty() }

    /** يعادل CURRENT[0] بالويب (آخر سجل أُدخل، بحسب ts وليس بحسب متغيّر جلسة مؤقت). */
    suspend fun findMostRecent(): BeneficiaryRecord? = withContext(Dispatchers.IO) { dao.findMostRecent() }

    /**
     * يعادل buildBackupBlob() بالويب — يبني JSON كاملاً (النسخة الحالية + كل الأشهر
     * المؤرشفة) من الجدول الواحد المسطَّح، بإعادة تجميعه لنفس شكل مصفوفة الأرشيف
     * المتوقَّعة من نسخة الويب، للتوافق التبادلي. يستخدم BackupRecord (DTO مستقل
     * عن كيان Room) لا الكيان مباشرة، لتفادي تسريب عمود monthKey الداخلي.
     */
    suspend fun exportAllAsJson(): String = withContext(Dispatchers.IO) {
        val all = dao.getAllRecords()
        val nowKey = MonthKey.currentKey()
        val current = all.filter { it.monthKey == nowKey }.map { it.toBackupRecord() }
        val archives = all.filter { it.monthKey != nowKey }
            .groupBy { it.monthKey }
            .map { (key, records) ->
                ArchiveMonth(
                    key = key,
                    label = MonthKey.label(key),
                    archivedAt = System.currentTimeMillis(),
                    records = records.map { it.toBackupRecord() }
                )
            }
        val blob = BackupBlob(
            exportedAt = Instant.now().toString(),
            current = current,
            archives = archives
        )
        Json.encodeToString(BackupBlob.serializer(), blob)
    }

    /**
     * يعادل استيراد ملف JSON بالويب (handleJsonImport → ingestRecords): يقرأ ملفاً
     * بنفس صيغة BackupBlob (سواء أُنتج من هذا التطبيق أو من نسخة الويب)، يسطّحه،
     * ويمرّره لـ ingest() — فتُطبَّق نفس ضمانات منع التكرار والتوجيه الصحيح للشهر.
     */
    suspend fun importFromJson(jsonContent: String): IngestResult = withContext(Dispatchers.IO) {
        val blob = lenientBackupJson.decodeFromString(BackupBlob.serializer(), jsonContent)
        ingest(blob.flattenToRaw())
    }

    /** يعادل findById() بالويب — يبحث عن هوية سواء كانت هوية مستفيد أو منيب. */
    suspend fun findDuplicates(nationalId: String): List<BeneficiaryRecord> =
        withContext(Dispatchers.IO) { dao.findByNationalId(nationalId) }
}
