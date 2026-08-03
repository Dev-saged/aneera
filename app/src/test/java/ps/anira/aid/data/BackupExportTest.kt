package ps.anira.aid.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ps.anira.aid.backup.BackupBlob
import java.util.Calendar

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class BackupExportTest {

    private lateinit var db: AniraDatabase
    private lateinit var repo: Repository

    @Before
    fun setUp() {
        db = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            AniraDatabase::class.java
        ).allowMainThreadQueries().build()
        repo = Repository(db.recordDao())
    }

    @After
    fun tearDown() { db.close() }

    private fun prevMonthTs(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 10)
        return cal.timeInMillis
    }

    @Test
    fun `التصدير يقسّم السجلات الحالية عن المؤرشفة بشكل صحيح ويُعاد تحليله بلا فقدان بيانات`() = runTest {
        val oldTs = prevMonthTs()
        val nowTs = System.currentTimeMillis()

        repo.ingest(
            listOf(
                RawIncomingRecord("A-1", "محمد", "111111111", "أحمد", "211111111", "أب", false, nowTs),
                RawIncomingRecord("A-2", "سارة", "222222222", "ليلى", "222222221", "أم", true, oldTs),
                RawIncomingRecord("A-3", "خالد", "333333333", "منى", "333333331", "أخ", false, oldTs),
            )
        )

        val json = repo.exportAllAsJson()
        val blob = Json.decodeFromString(BackupBlob.serializer(), json)

        assertEquals("app يجب أن يكون anira", "anira", blob.app)
        assertEquals("سجل واحد بالشهر الحالي", 1, blob.current.size)
        assertEquals("سجل واحد بالحقل الحالي هو محمد", "محمد", blob.current.first().benName)

        assertEquals("شهر أرشيف واحد فقط (كلا السجلين القديمين بنفس الشهر)", 1, blob.archives.size)
        assertEquals("سجلا الأرشيف كلاهما بنفس الشهر", 2, blob.archives.first().records.size)

        // التأكد من عدم فقدان حقل abroad تحديداً أثناء التسلسل/التحليل (Boolean عرضة للأخطاء بالتسلسل اليدوي)
        val sara = blob.archives.first().records.first { it.benName == "سارة" }
        assertTrue("حقل abroad يجب أن يبقى true بعد التسلسل الكامل", sara.abroad)
    }

    @Test
    fun `استيراد JSON بنفس شكل تصدير الويب بالضبط (بلا حقل monthKey) ينجح فعلياً`() = runTest {
        // هذا النص مكتوب يدوياً بنفس الحقول بالضبط التي تنتجها buildBackupBlob() بالويب —
        // لا وجود لحقل monthKey إطلاقاً (وهو تفصيل تخزين داخلي خاص بالنسخة الأصلية فقط).
        val webStyleJson = """
            {
              "app": "anira",
              "v": 2,
              "exportedAt": "2026-08-03T10:00:00.000Z",
              "by": "web",
              "current": [
                {"id":"W-1","benName":"وائل","benId":"444444444","depName":"سلمى","depId":"444444441","relation":"ابن","abroad":false,"ts":1754208000000}
              ],
              "archives": []
            }
        """.trimIndent()

        val result = repo.importFromJson(webStyleJson)
        assertEquals("لازم يضيف السجل الوحيد بنجاح رغم غياب monthKey من ملف الويب", 1, result.added)
        assertEquals(0, result.duplicate)
    }
}
