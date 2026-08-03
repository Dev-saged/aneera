package ps.anira.aid.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface RecordDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(record: BeneficiaryRecord): Long

    /**
     * يعادل ingestRecords() بالويب من ناحية التكرار: IGNORE على تعارض المفتاح
     * الأساسي (id) يعني أي سجل بنفس id موجود مسبقاً يُتجاهل بصمت بدل استبداله —
     * هذا هو أساس منع التكرار عند الدمج، لكن منطق العد (كم أُضيف / كم مكرَّر)
     * يحتاج بناء بطبقة أعلى (Repository) لاحقاً، مطابقاً تماماً لسيناريوهات
     * الاختبار الـ16 التي تحقّقنا منها على نسخة الويب — يجب إعادة نفس الاختبارات
     * هنا (JUnit) قبل الاعتماد على هذا الجزء بالحقل، لا تخمين.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BeneficiaryRecord>): List<Long>

    @Query("SELECT * FROM records WHERE monthKey = :monthKey ORDER BY ts DESC")
    fun observeByMonth(monthKey: String): Flow<List<BeneficiaryRecord>>

    @Query("SELECT DISTINCT monthKey FROM records WHERE monthKey != :currentMonthKey ORDER BY monthKey DESC")
    fun observeArchivedMonths(currentMonthKey: String): Flow<List<String>>

    @Query("SELECT * FROM records WHERE benId = :nationalId OR depId = :nationalId ORDER BY ts DESC")
    suspend fun findByNationalId(nationalId: String): List<BeneficiaryRecord>

    @Query("SELECT COUNT(*) FROM records WHERE monthKey = :currentMonthKey")
    fun observeCurrentCount(currentMonthKey: String): Flow<Int>

    @Delete
    suspend fun delete(record: BeneficiaryRecord)

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): BeneficiaryRecord?
}
