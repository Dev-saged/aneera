package ps.anira.aid.data

/**
 * يعادل idChecksumOk() بالنسخة الويب حرفياً (فُحصت هناك ضد 200 ألف رقم عشوائي
 * ومقابل مرجع Luhn-variant مستقل). نفس التحفظ المذكور بالويب ساري هنا: تحذير
 * خفيف غير معطِّل — ما في مصدر يؤكد قطعياً انطباقها على كل رقم هوية فلسطيني
 * بلا استثناء، فلا تُستخدم كقفل تسجيل إلزامي.
 */
object IdChecksum {
    fun isOk(id: String): Boolean {
        if (!Regex("^\\d{9}$").matches(id)) return true // غير مكتمل: لا تحذير بعد
        var sum = 0
        for (i in 0 until 9) {
            var d = (id[i] - '0') * ((i % 2) + 1)
            if (d > 9) d -= 9
            sum += d
        }
        return sum % 10 == 0
    }
}
