package net.dimanss47.swpersona

import android.content.Context
import androidx.paging.DataSource
import androidx.room.Room
import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
import io.reactivex.Single
import java.util.*


class OrderedPersonDetails(
    private val details: SortedMap<String, PersonalDetail>
) : SortedMap<String, PersonalDetail> by details {
    fun copy() = OrderedPersonDetails(TreeMap(details))
}

data class ExternalPersonalDetail(
    @SerializedName("name") val nameField: String?,
    val title: String?
) {
    val name: String?
        get() = nameField ?: title ?: ""
}

data class Person(
    val name: String,
    val gender: String,
    @SerializedName("birth_year") val birthYear: String,
    val url: String
)

object PeopleRepository {
    private val swapi = SwApi.create()
    private var history: History? = null

    fun init(context: Context) {
        if(history == null) {
            history = Room.databaseBuilder(
                context,
                HistoryDb::class.java,
                getHistoryDbName(context)
            ).build(
            ).history()
        }
    }

    fun getPeopleList(searchTerm: String): DataSource.Factory<String?, Person> =
        swapi.getPeopleList(searchTerm)

    fun getPeopleHistoryList(): DataSource.Factory<Int, HistoryItem> {
        if(history == null) throw HistoryNotInitializedError()
        return history!!.getHistory()
    }

    fun getHistoryCacheEntry(url: String): Single<HistoryItem> {
        if(history == null) throw HistoryNotInitializedError()
        return history!!.getHistoryCacheEntry(url)
    }

    fun getPerson(url: String): Observable<OrderedPersonDetails> =
        swapi.getPerson(url)
}

class HistoryNotInitializedError : Throwable("call PeopleRepository.init(context)")
