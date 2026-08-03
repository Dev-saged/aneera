package ps.anira.aid.ui

/** مطابقة حرفياً لقائمة relationSelect بالنسخة الويب. */
object RelationOptions {
    const val CUSTOM = "أخرى"

    val all = listOf(
        "أب", "أم", "ابن", "ابنة", "أخ", "أخت", "زوج", "زوجة",
        "عم", "عمة", "خال", "خالة",
        "ابن عم", "ابن عمة", "ابن خال", "ابن خالة", "ابن أخ", "ابن أخت",
        "نسيب", "عديل", CUSTOM
    )
}
