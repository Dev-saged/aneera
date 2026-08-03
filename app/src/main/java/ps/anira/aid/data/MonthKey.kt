package ps.anira.aid.data

import java.util.Calendar
import java.util.Locale

/** يطابق monthKeyOf()/monthLabel() بالنسخة الويب (anira-system.html) حرفياً. */
object MonthKey {

    private val arMonths = arrayOf(
        "يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو",
        "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر"
    )

    /** "yyyy-MM" — نفس صيغة monthKeyOf(ts) بالويب. */
    fun of(epochMillis: Long): String {
        val cal = Calendar.getInstance(Locale.US).apply { timeInMillis = epochMillis }
        val year = cal.get(Calendar.YEAR)
        val month = cal.get(Calendar.MONTH) + 1
        return String.format(Locale.US, "%04d-%02d", year, month)
    }

    fun currentKey(): String = of(System.currentTimeMillis())

    /** "أغسطس 2026" — نفس صيغة monthLabel(key) بالويب. */
    fun label(key: String): String {
        val parts = key.split("-")
        if (parts.size != 2) return key
        val monthIdx = parts[1].toIntOrNull()?.minus(1) ?: return key
        if (monthIdx !in arMonths.indices) return key
        return "${arMonths[monthIdx]} ${parts[0]}"
    }
}
