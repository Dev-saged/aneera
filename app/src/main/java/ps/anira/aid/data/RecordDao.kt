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
     * IGNORE على تعارض المفتاح الأساسي (id): أي سجل بنفس id موجود مسبقاً يُتجاهل
     * بصمت بدل استبداله — هذا أساس منع التكرار عند الدمج. منطق العدّ (كم أُضيف/
     * كم مكرَّر) مبني بـ Repository.ingest()، ومُختبَر فعلياً بـ RepositoryTest.kt
     * (يعيد إنتاج نفس السيناريوهات الـ16 المتحقَّق منها بنسخة الويب).
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(records: List<BeneficiaryRecord>): List<Long>

    @Query("SELECT * FROM records WHERE monthKey = :monthKey ORDER BY ts DESC")
    fun observeByMonth(monthKey: String): Flow<List<BeneficiaryRecord>>

    @Query("SELECT * FROM records WHERE monthKey = :monthKey ORDER BY ts DESC")
    suspend fun observeByMonthSync(monthKey: String): List<BeneficiaryRecord>

    @Query("SELECT DISTINCT monthKey FROM records WHERE monthKey != :currentMonthKey ORDER BY monthKey DESC")
    fun observeArchivedMonths(currentMonthKey: String): Flow<List<String>>

    @Query("SELECT * FROM records WHERE benId = :nationalId OR depId = :nationalId ORDER BY ts DESC")
    suspend fun findByNationalId(nationalId: String): List<BeneficiaryRecord>

    @Query("SELECT COUNT(*) FROM records WHERE monthKey = :currentMonthKey")
    fun observeCurrentCount(currentMonthKey: String): Flow<Int>

    @Delete
    suspend fun delete(record: BeneficiaryRecord)

    @androidx.room.Update
    suspend fun update(record: BeneficiaryRecord)

    @Query("SELECT COUNT(*) FROM records WHERE monthKey = :monthKey")
    suspend fun countInMonth(monthKey: String): Int

    @Query("SELECT COUNT(*) FROM records")
    suspend fun totalCount(): Int

    @Query("SELECT relation FROM records GROUP BY relation ORDER BY COUNT(*) DESC LIMIT 1")
    suspend fun topRelation(): String?

    @Query("SELECT COUNT(*) FROM records WHERE abroad = 1")
    suspend fun abroadCount(): Int

    @Query("SELECT * FROM records WHERE id = :id LIMIT 1")
    suspend fun findById(id: String): BeneficiaryRecord?

    @Query("SELECT id FROM records")
    suspend fun getAllIds(): List<String>

    @Query("SELECT * FROM records ORDER BY ts DESC")
    suspend fun getAllRecords(): List<BeneficiaryRecord>

    @Query("SELECT * FROM records ORDER BY ts DESC LIMIT 1")
    suspend fun findMostRecent(): BeneficiaryRecord?
}
