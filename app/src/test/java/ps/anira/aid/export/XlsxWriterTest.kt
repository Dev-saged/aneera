package ps.anira.aid.export

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ps.anira.aid.data.BeneficiaryRecord
import java.io.ByteArrayInputStream
import java.util.zip.ZipInputStream

/**
 * تصميم بنية الـXLSX نفسه فُحص خارجياً ببايثون (openpyxl) قبل كتابة هذا الكود
 * (انظر ملاحظة الهندسة بالتسليم)، لكن هذا الاختبار يتحقق فعلياً من مخرجات
 * الكود الحقيقي هنا عبر CI: بنية ZIP صحيحة، كل الأجزاء المطلوبة موجودة،
 * والتهريب الصحيح للأحرف الخطرة (& < > " ') داخل XML.
 */
class XlsxWriterTest {

    private fun sample() = BeneficiaryRecord(
        id = "A-1", benName = "محمد <O'Brien> & \"Co\"", benId = "111111111",
        depName = "سارة", depId = "222222222", relation = "أب", abroad = true,
        ts = 1754208000000L, monthKey = "2026-08"
    )

    @Test
    fun `الملف الناتج زيب صالح ويحتوي كل الأجزاء المطلوبة`() {
        val bytes = XlsxWriter.build(listOf(sample()))
        val entries = mutableSetOf<String>()
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                entries.add(entry.name)
                entry = zip.nextEntry
            }
        }
        val required = setOf(
            "[Content_Types].xml", "_rels/.rels", "xl/workbook.xml",
            "xl/_rels/workbook.xml.rels", "xl/styles.xml", "xl/worksheets/sheet1.xml"
        )
        assertTrue("كل الأجزاء المطلوبة موجودة: $entries", entries.containsAll(required))
    }

    @Test
    fun `الأحرف الخطرة بـXML تُهرَّب بشكل صحيح ولا تكسر البنية`() {
        val bytes = XlsxWriter.build(listOf(sample()))
        val sheetXml = readEntry(bytes, "xl/worksheets/sheet1.xml")

        // يجب ألا يظهر '<' أو '&' خاماً داخل محتوى الاسم (كان سيكسر الـXML)
        assertTrue("علامة < يجب أن تكون مهرَّبة كـ&lt;", sheetXml.contains("&lt;O&apos;Brien&gt;"))
        assertTrue("علامة & يجب أن تكون مهرَّبة كـ&amp;", sheetXml.contains("&amp;"))
        assertTrue("علامة \" يجب أن تكون مهرَّبة كـ&quot;", sheetXml.contains("&quot;Co&quot;"))

        // 10 أعمدة بالرأس + 10 بصف البيانات = 20 وسم <c
        val cCount = Regex("<c ").findAll(sheetXml).count()
        assertEquals(20, cCount)
    }

    @Test
    fun `عمود خارج البلاد يعكس القيمة الصحيحة نعم لا`() {
        val bytes = XlsxWriter.build(listOf(sample()))
        val sheetXml = readEntry(bytes, "xl/worksheets/sheet1.xml")
        assertTrue("abroad=true يجب أن يظهر كـ'نعم'", sheetXml.contains("<t>نعم</t>"))
    }

    private fun readEntry(bytes: ByteArray, name: String): String {
        ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
            var entry = zip.nextEntry
            while (entry != null) {
                if (entry.name == name) return zip.readBytes().toString(Charsets.UTF_8)
                entry = zip.nextEntry
            }
        }
        throw AssertionError("Entry not found: $name")
    }
}
