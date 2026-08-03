package ps.anira.aid.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * يطابق حرفياً حقول normalizeRec() بنسخة الويب (anira-system.html):
 * id, benName, benId, depName, depId, relation, abroad, ts.
 *
 * تبسيط متعمَّد عن البنية الأصلية: النسخة الويب تحمل مصفوفة ARCHIVES منفصلة،
 * كل عنصر فيها كائن شهر يحوي مصفوفة records. هذا مناسب لـ JSON/IndexedDB لكنه
 * غير طبيعي لقاعدة علائقية. هنا: جدول واحد فقط، بعمود monthKey (مثل "2026-08")
 * محسوب من ts وقت الإدخال. "السجل الحالي" = الصفوف اللي monthKey فيها يطابق
 * الشهر التقويمي الفعلي الآن؛ الباقي = "مؤرشف". هذا يبسّط طبقة البيانات
 * ويطابق تماماً نفس المنطق الذي أصلحناه بالويب (v1.2.0): التوجيه حسب توقيت
 * السجل نفسه مقارنة بالشهر الحالي الفعلي — مو حسب وجود أرشيف محلي من عدمه.
 */
@Entity(
    tableName = "records",
    indices = [Index(value = ["monthKey"]), Index(value = ["benId"]), Index(value = ["depId"])]
)
data class BeneficiaryRecord(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "benName")
    val benName: String,

    @ColumnInfo(name = "benId")
    val benId: String,

    @ColumnInfo(name = "depName")
    val depName: String,

    @ColumnInfo(name = "depId")
    val depId: String,

    val relation: String,

    val abroad: Boolean = false,

    /** Epoch millis — نفس دلالة Date.now() بالنسخة الويب. */
    val ts: Long,

    /** مشتق من ts وقت الإدخال، صيغة "yyyy-MM" — يعادل monthKeyOf() بالويب. */
    @ColumnInfo(name = "monthKey")
    val monthKey: String
)
