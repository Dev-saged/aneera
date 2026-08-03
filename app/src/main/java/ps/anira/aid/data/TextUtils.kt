package ps.anira.aid.data

/** يعادل onlyDigits() بالنسخة الويب حرفياً. */
fun onlyDigits(s: String): String = s.filter { it.isDigit() }
