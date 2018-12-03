package net.dimanss47.swpersona

import android.content.Context
import androidx.paging.DataSource
import androidx.room.*
import io.reactivex.Single
import java.util.*

@Database(version = 1, entities = [ History::class ])
abstract class HistoryDb : RoomDatabase() {
    abstract fun history(): History
}

@Entity(indices = [Index(value = ["url"], unique = true)])
data class HistoryItem(
    val name: String,
    val birthYear: String,
    val gender: String,
    @PrimaryKey(autoGenerate = true)
    val url: String,

    // json
    val other: String,

    var created_at: Date = Date()
)

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

private const val DB_NAME = "history_cache.db"

fun getHistoryDbName(context: Context): String {
    return "${context.cacheDir.canonicalPath}/$DB_NAME"
}
