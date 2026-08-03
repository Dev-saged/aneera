package ps.anira.aid.export

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import ps.anira.aid.data.BeneficiaryRecord
import ps.anira.aid.data.MonthKey
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * يبني ملف PDF جدولي حقيقي عبر android.graphics.pdf.PdfDocument (Canvas قياسي
 * بأندرويد، لا بناء بايتات PDF يدوياً كما احتاجت نسخة الويب) — نفس الأعمدة
 * العشرة المطابقة لتصدير Excel. يستخدم StaticLayout بدل Canvas.drawText
 * المباشرة تحديداً لضمان تنضيد ثنائي الاتجاه (bidi) صحيح للنص العربي داخل
 * كل خلية (drawText وحدها لا تُشكِّل حروف الاتصال العربية بشكل صحيح ضمن عرض محدود).
 */
object PdfWriter {

    // A4 landscape بالنقاط (72 نقطة/إنش)
    private const val PAGE_W = 842
    private const val PAGE_H = 595
    private const val MARGIN = 28f
    private const val ROW_HEIGHT = 28f
    private const val HEADER_HEIGHT = 32f
    private const val TITLE_HEIGHT = 34f

    private val headers = listOf(
        "الرقم", "التاريخ والوقت", "الشهر", "اسم المستفيد", "هوية المستفيد",
        "اسم المنيب", "هوية المنيب", "الصلة", "خارج البلاد", "معرّف السجل"
    )
    // نسب عرض تقريبية تجمع 1.0 — تُضرَب بعرض الجدول الفعلي
    private val colWeights = listOf(0.05f, 0.13f, 0.10f, 0.14f, 0.10f, 0.14f, 0.10f, 0.09f, 0.08f, 0.07f)
    private val dateFmt = SimpleDateFormat("yyyy/MM/dd HH:mm", Locale.US)

    fun build(records: List<BeneficiaryRecord>, title: String): ByteArray {
        val document = PdfDocument()
        val tableWidth = PAGE_W - 2 * MARGIN
        val colWidths = colWeights.map { it * tableWidth }

        val headerPaint = TextPaint().apply {
            isAntiAlias = true; textSize = 11f; color = Color.WHITE; isFakeBoldText = true
        }
        val cellPaint = TextPaint().apply { isAntiAlias = true; textSize = 10f; color = Color.BLACK }
        val titlePaint = Paint().apply { isAntiAlias = true; textSize = 16f; color = Color.BLACK; isFakeBoldText = true }
        val linePaint = Paint().apply { color = Color.rgb(226, 232, 240); strokeWidth = 0.75f }
        val headerBgPaint = Paint().apply { color = Color.rgb(15, 23, 42) } // يطابق --ink بالويب

        var page: PdfDocument.Page? = null
        var canvas: Canvas? = null
        var y = 0f
        var pageNum = 0

        fun newPage() {
            page?.let { document.finishPage(it) }
            pageNum++
            val info = PdfDocument.PageInfo.Builder(PAGE_W, PAGE_H, pageNum).create()
            page = document.startPage(info)
            canvas = page!!.canvas
            y = MARGIN

            canvas!!.drawText(title, PAGE_W - MARGIN, y + TITLE_HEIGHT - 10f, titlePaint.apply { textAlign = Paint.Align.RIGHT })
            y += TITLE_HEIGHT

            // صف الرأس بخلفية داكنة
            canvas!!.drawRect(MARGIN, y, MARGIN + tableWidth, y + HEADER_HEIGHT, headerBgPaint)
            var x = MARGIN + tableWidth
            headers.forEachIndexed { i, h ->
                x -= colWidths[i]
                drawCell(canvas!!, h, x, y, colWidths[i], HEADER_HEIGHT, headerPaint)
            }
            y += HEADER_HEIGHT
        }

        newPage()

        records.forEachIndexed { idx, r ->
            if (y + ROW_HEIGHT > PAGE_H - MARGIN) newPage()

            val values = listOf(
                (idx + 1).toString(), dateFmt.format(Date(r.ts)), MonthKey.label(r.monthKey),
                r.benName, r.benId, r.depName, r.depId, r.relation,
                if (r.abroad) "نعم" else "لا", r.id
            )
            var x = MARGIN + tableWidth
            values.forEachIndexed { i, v ->
                x -= colWidths[i]
                drawCell(canvas!!, v, x, y, colWidths[i], ROW_HEIGHT, cellPaint)
            }
            canvas!!.drawLine(MARGIN, y + ROW_HEIGHT, MARGIN + tableWidth, y + ROW_HEIGHT, linePaint)
            y += ROW_HEIGHT
        }

        page?.let { document.finishPage(it) }

        val out = java.io.ByteArrayOutputStream()
        document.writeTo(out)
        document.close()
        return out.toByteArray()
    }

    /** يرسم نص خلية عبر StaticLayout (تشكيل bidi/عربي صحيح) بدل Canvas.drawText المباشرة. */
    private fun drawCell(canvas: Canvas, text: String, x: Float, y: Float, width: Float, height: Float, paint: TextPaint) {
        val layout = StaticLayout.Builder
            .obtain(text, 0, text.length, paint, width.toInt().coerceAtLeast(1))
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_RTL)
            .setMaxLines(2)
            .setEllipsize(android.text.TextUtils.TruncateAt.END)
            .build()
        canvas.save()
        canvas.translate(x + 4f, y + (height - layout.height) / 2f)
        layout.draw(canvas)
        canvas.restore()
    }
}
