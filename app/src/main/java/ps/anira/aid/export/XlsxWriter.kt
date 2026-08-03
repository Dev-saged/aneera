package ps.anira.aid.export

import ps.anira.aid.data.BeneficiaryRecord
import ps.anira.aid.data.MonthKey
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

/**
 * يبني ملف .xlsx كامل وصالح بلا أي مكتبة خارجية — عبر java.util.zip المدمج
 * بالـJVM (بعكس نسخة الويب التي احتاجت محرك ضغط يدوي كامل، لأن أندرويد/JVM
 * توفّر Deflater جاهزاً). البنية (Content_Types/rels/workbook/styles/worksheet)
 * صُمِّمت وفُحصت مسبقاً ببايثون (openpyxl) قبل كتابة هذا الكود، للتأكد من أن
 * الملف الناتج يُفتح بلا أي تحذير أو خطأ حقيقي، لا افتراضاً.
 *
 * 10 أعمدة، مطابقة تماماً لتصدير الويب: الرقم/تاريخ ووقت التسجيل/الشهر/
 * اسم المستفيد/هوية المستفيد/اسم المنيب/هوية المنيب/الصلة/خارج البلاد/معرّف السجل.
 */
object XlsxWriter {

    private val headers = listOf(
        "الرقم", "تاريخ ووقت التسجيل", "الشهر", "اسم المستفيد", "هوية المستفيد",
        "اسم المنيب", "هوية المنيب", "الصلة", "خارج البلاد", "معرّف السجل"
    )
    private val colWidths = listOf(6, 20, 14, 24, 14, 24, 14, 14, 12, 22)
    private val dateFmt = SimpleDateFormat("yyyy/MM/dd - HH:mm", Locale.US)

    /** يهرّب النص لسياق XML (وسم <t>) — إلزامي، بما أن أسماء المستفيدين نص حر قد يحوي &lt; &amp; إلخ. */
    private fun esc(s: String): String = s
        .replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;")
        .replace("'", "&apos;")

    private fun colLetter(index0: Int): String {
        var n = index0 + 1
        val sb = StringBuilder()
        while (n > 0) {
            val rem = (n - 1) % 26
            sb.insert(0, ('A' + rem))
            n = (n - rem - 1) / 26
        }
        return sb.toString()
    }

    fun build(records: List<BeneficiaryRecord>): ByteArray {
        val sheetXml = buildSheetXml(records)
        val out = ByteArrayOutputStream()
        ZipOutputStream(out).use { zip ->
            writeEntry(zip, "[Content_Types].xml", CONTENT_TYPES)
            writeEntry(zip, "_rels/.rels", ROOT_RELS)
            writeEntry(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS)
            writeEntry(zip, "xl/workbook.xml", WORKBOOK_XML)
            writeEntry(zip, "xl/styles.xml", STYLES_XML)
            writeEntry(zip, "xl/worksheets/sheet1.xml", sheetXml)
        }
        return out.toByteArray()
    }

    private fun writeEntry(zip: ZipOutputStream, name: String, content: String) {
        zip.putNextEntry(ZipEntry(name))
        zip.write(content.toByteArray(Charsets.UTF_8))
        zip.closeEntry()
    }

    private fun buildSheetXml(records: List<BeneficiaryRecord>): String {
        val sb = StringBuilder()
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>")
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">")
        sb.append("<sheetViews><sheetView rightToLeft=\"1\" workbookViewId=\"0\"/></sheetViews>")
        sb.append("<cols>")
        colWidths.forEachIndexed { i, w ->
            sb.append("<col min=\"${i + 1}\" max=\"${i + 1}\" width=\"$w\" customWidth=\"1\"/>")
        }
        sb.append("</cols>")
        sb.append("<sheetData>")

        // صف الرأس (عريض، الأنماط s="1")
        sb.append("<row r=\"1\">")
        headers.forEachIndexed { i, h ->
            sb.append("<c r=\"${colLetter(i)}1\" t=\"inlineStr\" s=\"1\"><is><t>${esc(h)}</t></is></c>")
        }
        sb.append("</row>")

        // صفوف البيانات
        records.forEachIndexed { idx, r ->
            val rowNum = idx + 2
            val values = listOf(
                (idx + 1).toString(),
                dateFmt.format(Date(r.ts)),
                MonthKey.label(r.monthKey),
                r.benName,
                r.benId,
                r.depName,
                r.depId,
                r.relation,
                if (r.abroad) "نعم" else "لا",
                r.id
            )
            sb.append("<row r=\"$rowNum\">")
            values.forEachIndexed { i, v ->
                sb.append("<c r=\"${colLetter(i)}$rowNum\" t=\"inlineStr\"><is><t>${esc(v)}</t></is></c>")
            }
            sb.append("</row>")
        }

        sb.append("</sheetData></worksheet>")
        return sb.toString()
    }

    private const val CONTENT_TYPES = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">
<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>
<Default Extension="xml" ContentType="application/xml"/>
<Override PartName="/xl/workbook.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml"/>
<Override PartName="/xl/worksheets/sheet1.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml"/>
<Override PartName="/xl/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml"/>
</Types>"""

    private const val ROOT_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="xl/workbook.xml"/>
</Relationships>"""

    private const val WORKBOOK_RELS = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">
<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet" Target="worksheets/sheet1.xml"/>
<Relationship Id="rId2" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>
</Relationships>"""

    private const val WORKBOOK_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<workbook xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main" xmlns:r="http://schemas.openxmlformats.org/officeDocument/2006/relationships">
<sheets>
<sheet name="السجلات" sheetId="1" r:id="rId1"/>
</sheets>
</workbook>"""

    private const val STYLES_XML = """<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
<styleSheet xmlns="http://schemas.openxmlformats.org/spreadsheetml/2006/main">
<fonts count="2"><font><sz val="11"/><name val="Calibri"/></font><font><b/><sz val="11"/><name val="Calibri"/></font></fonts>
<fills count="1"><fill><patternFill patternType="none"/></fill></fills>
<borders count="1"><border><left/><right/><top/><bottom/><diagonal/></border></borders>
<cellStyleXfs count="1"><xf numFmtId="0" fontId="0" fillId="0" borderId="0"/></cellStyleXfs>
<cellXfs count="2">
<xf numFmtId="0" fontId="0" fillId="0" borderId="0" xfId="0"/>
<xf numFmtId="0" fontId="1" fillId="0" borderId="0" xfId="0" applyFont="1"/>
</cellXfs>
<cellStyles count="1"><cellStyle name="Normal" xfId="0" builtinId="0"/></cellStyles>
</styleSheet>"""
}
