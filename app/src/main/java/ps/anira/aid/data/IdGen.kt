package ps.anira.aid.data

/** يعادل genId()/safeId() بالنسخة الويب حرفياً — نفس صيغة المعرّف ونفس قاعدة التحقق. */
object IdGen {
    private val safeIdPattern = Regex("^[A-Za-z0-9_-]{1,64}$")
    private const val CHARS = "0123456789abcdefghijklmnopqrstuvwxyz"

    fun generate(): String {
        val ts = java.lang.Long.toString(System.currentTimeMillis(), 36)
        val rand = (1..5).map { CHARS.random() }.joinToString("")
        return "A-$ts-$rand"
    }

    /** يرفض أي قيمة لا تطابق الصيغة الآمنة (بما فيها محاولات حقن عبر ملف مستورَد) ويولّد بديلاً آمناً. */
    fun safe(id: String?): String =
        if (id != null && safeIdPattern.matches(id)) id else generate()
}
