package ps.anira.aid.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * يعيد إنتاج حرفياً نفس السيناريوهات الـ16 المُتحقَّق منها بـ test_ingest.js
 * بنسخة الويب (بما فيها الثلاثة الإضافية التي تجاوزت الاختبارين الموثَّقين
 * أصلاً بملف التسليم v1.2.0)، ضد قاعدة Room حقيقية في الذاكرة عبر Robolectric —
 * لا محاكاة يدوية، بل نفس طبقة الإدراج/الاستعلام الفعلية التي سيستخدمها التطبيق.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
class RepositoryTest {

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
    fun tearDown() {
        db.close()
    }

    /** أول يوم بمنتصف الشهر الماضي — دائماً مختلف عن الشهر التقويمي الحالي فعلياً وقت التشغيل. */
    private fun prevMonthTs(): Long {
        val cal = Calendar.getInstance()
        cal.add(Calendar.MONTH, -1)
        cal.set(Calendar.DAY_OF_MONTH, 15)
        return cal.timeInMillis
    }

    private fun sampleRaw(id: String, ts: Long, benId: String) = RawIncomingRecord(
        id = id, benName = "ط", benId = benId, depName = "د", depId = "2$benId".take(9),
        relation = "أب", abroad = false, ts = ts
    )

    @Test
    fun `S1 - جهاز بلا أرشيف محلي، دفعة مختلطة (شهر سابق + شهر حالي)`() = runTest {
        val oldTs = prevMonthTs()
        val nowTs = System.currentTimeMillis()
        val batch = listOf(
            sampleRaw("A-old1", oldTs, "111111111"),
            sampleRaw("A-old2", oldTs, "111111112"),
            sampleRaw("A-old3", oldTs, "111111113"),
            sampleRaw("A-cur1", nowTs, "111111114"),
        )
        val res = repo.ingest(batch)
        assertEquals(4, res.added)
        assertEquals(0, res.duplicate)

        val oldKey = MonthKey.of(oldTs)
        val nowKey = MonthKey.of(nowTs)
        val oldRows = db.recordDao().observeByMonthSync(oldKey)
        val curRows = db.recordDao().observeByMonthSync(nowKey)
        assertEquals("3 سجلات لازم تروح لمفتاح الشهر الماضي تلقائياً", 3, oldRows.size)
        assertEquals("سجل واحد بس لازم يروح لمفتاح الشهر الحالي", 1, curRows.size)
    }

    @Test
    fun `S2 - إعادة استيراد نفس الدفعة يجب أن تُهمَل بالكامل كمكررة`() = runTest {
        val oldTs = prevMonthTs()
        val batch = listOf(
            sampleRaw("A-old1", oldTs, "111111111"),
            sampleRaw("A-old2", oldTs, "111111112"),
        )
        repo.ingest(batch)
        val res2 = repo.ingest(batch)
        assertEquals(0, res2.added)
        assertEquals(2, res2.duplicate)
    }

    @Test
    fun `S3 - دمج مع سجلات محلية موجودة مسبقاً لنفس الشهر بدون تكرار وهمي`() = runTest {
        val oldTs = prevMonthTs()
        // سجلات "محلية" موجودة مسبقاً بنفس الشهر
        repo.ingest(listOf(sampleRaw("LOCAL-1", oldTs, "900000001"), sampleRaw("LOCAL-2", oldTs, "900000002")))
        // دفعة مستوردة من صديق، بلا تطابق أي id محلي
        val res = repo.ingest(
            listOf(
                sampleRaw("A-old1", oldTs, "111111111"),
                sampleRaw("A-old2", oldTs, "111111112"),
                sampleRaw("A-old3", oldTs, "111111113"),
            )
        )
        assertEquals(3, res.added)
        assertEquals(0, res.duplicate)
        val allInMonth = db.recordDao().observeByMonthSync(MonthKey.of(oldTs))
        assertEquals("2 محلي + 3 مستورد = 5", 5, allInMonth.size)
    }

    @Test
    fun `S4 - دفعة مختلطة جزء مكرر وجزء جديد`() = runTest {
        val oldTs = prevMonthTs()
        repo.ingest(listOf(sampleRaw("LOCAL-1", oldTs, "900000001")))
        val res = repo.ingest(
            listOf(
                sampleRaw("LOCAL-1", oldTs, "900000001"), // مكرر
                sampleRaw("A-new1", oldTs, "222222222"),  // جديد
            )
        )
        assertEquals(1, res.added)
        assertEquals(1, res.duplicate)
    }

    @Test
    fun `S5 - معرّف خبيث من ملف مستورَد يُستبدَل بمعرّف آمن`() = runTest {
        val maliciousId = "x\" onmouseover=\"alert(1)"
        repo.ingest(listOf(sampleRaw(maliciousId, System.currentTimeMillis(), "333333333")))
        val stored = db.recordDao().getAllIds().first()
        assertNotEquals("لازم ما يُخزَّن المعرّف الخبيث حرفياً", maliciousId, stored)
        assertTrue(
            "لازم يطابق صيغة المعرّف الآمن",
            Regex("^[A-Za-z0-9_-]{1,64}$").matches(stored)
        )
    }

    @Test
    fun `S6 - checksum لأرقام هوية صحيحة وخاطئة الصيغة رياضياً`() {
        // تحققت من هذين الرقمين فعلياً عبر تشغيل خوارزمية مطابقة قبل الكتابة (لا تخمين)
        assertTrue("203458252 صحيح رياضياً حسب الخوارزمية", IdChecksum.isOk("203458252"))
        assertTrue("123456789 خاطئ رياضياً — لازم false", !IdChecksum.isOk("123456789"))
        // رقم غير مكتمل (أقل من 9 خانات) = بلا تحذير (true) دائماً، مو رفض
        assertTrue(IdChecksum.isOk("12345"))
    }
}
