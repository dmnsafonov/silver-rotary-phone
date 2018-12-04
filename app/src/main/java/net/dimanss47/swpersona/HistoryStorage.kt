package net.dimanss47.swpersona

import android.content.Context
import androidx.paging.DataSource
import androidx.room.*
import io.reactivex.Single
import java.util.*

@Database(version = 1, entities = [ HistoryItem::class ])
@TypeConverters(HistoryConverters::class)
abstract class HistoryDb : RoomDatabase() {
    abstract fun history(): History
}

@Entity(indices = [Index(value = ["url"], unique = true)])
data class HistoryItem(
    override val name: String,
    override val birthYear: String,
    override val gender: String,
    @PrimaryKey override val url: String,

    // json: ["key": "value"] representation of OrderedPersonalDetails
    val fullSerialized: String,

    var created_at: Date = Date()
) : AbstractPerson()

@Dao
interface History {
    @Insert
    fun insert(item: HistoryItem)

    @Delete
    fun delete(item: HistoryItem)

    @Query("SELECT * FROM HistoryItem ORDER BY created_at")
    fun getHistory(): DataSource.Factory<Int, HistoryItem>

    @Query("SELECT * FROM HistoryItem WHERE url = :url")
    fun getHistoryCacheEntry(url: String): Single<HistoryItem>
}

class HistoryConverters {
    @TypeConverter
    fun fromTimestamp(ts: Long): Date = Date(ts)

    @TypeConverter
    fun toTimestamp(date: Date): Long = date.time
}

private const val DB_NAME = "history_cache.db"

fun getHistoryDbName(context: Context): String {
    return "${context.cacheDir.canonicalPath}/$DB_NAME"
}
