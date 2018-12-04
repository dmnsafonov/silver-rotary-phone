package net.dimanss47.swpersona

import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.paging.DataSource
import androidx.recyclerview.widget.RecyclerView
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

abstract class AbstractPerson {
    abstract val name: String
    abstract val gender: String
    abstract val birthYear: String
    abstract val url: String
}

data class Person(
    override val name: String,
    override val gender: String,
    @SerializedName("birth_year") override val birthYear: String,
    override val url: String
) : AbstractPerson()

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

class PersonListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    val name: TextView = view.findViewById(R.id.name)
    val birthYear: TextView = view.findViewById(R.id.birth_year)
    val gender: TextView = view.findViewById(R.id.gender)
    var url: String? = null

    fun bindModel(person: AbstractPerson) {
        name.text = person.name
        birthYear.text = person.birthYear
        gender.text = person.gender
        url = person.url

    }

    fun setInProgress() {
        // TODO: better
        name.text = ""
        birthYear.text = ""
        gender.text = ""
        url = ""
    }
}

class HistoryNotInitializedError : Throwable("call PeopleRepository.init(context)")
