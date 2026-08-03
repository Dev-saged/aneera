package ps.anira.aid.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [BeneficiaryRecord::class], version = 1, exportSchema = false)
abstract class AniraDatabase : RoomDatabase() {
    abstract fun recordDao(): RecordDao

    companion object {
        @Volatile private var instance: AniraDatabase? = null

        fun get(context: Context): AniraDatabase =
            instance ?: synchronized(this) {
                instance ?: Room.databaseBuilder(
                    context.applicationContext,
                    AniraDatabase::class.java,
                    "anira.db"
                ).build().also { instance = it }
            }
    }
}
