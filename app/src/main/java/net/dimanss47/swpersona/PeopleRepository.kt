package net.dimanss47.swpersona

import android.annotation.SuppressLint
import android.content.Context
import android.view.View
import android.widget.TextView
import androidx.paging.DataSource
import androidx.recyclerview.widget.RecyclerView
import androidx.room.Room
import com.google.gson.annotations.SerializedName
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import java.util.*


class OrderedPersonDetails(
    private val details: SortedMap<String, PersonalDetail>
) : AbstractPerson(), SortedMap<String, PersonalDetail> by details {
    override val name: String
    override val gender: String
    override val birthYear: String
    override val url: String

    init {
        val nameVal = details["name"]
        name = if(nameVal?.isString == true) nameVal.getString() else ""

        val genderVal = details["gender"]
        gender = if(genderVal?.isString == true) genderVal.getString() else ""

        val birthYearVal = details["birth_year"]
        birthYear = if(birthYearVal?.isString == true) birthYearVal.getString() else ""

        val urlVal = details["url"]
        url = if(urlVal?.isString == true) urlVal.getString() else ""
    }

    fun copy() = OrderedPersonDetails(TreeMap(details))
}

object PersonDetailsEntriesComparator : Comparator<String> {
    private val PRIORITY_LIST = arrayListOf("name", "birth_year", "gender")

    override fun compare(o1: String?, o2: String?): Int {
        if(o1 == null || o2 == null) {
            throw IllegalArgumentException("PersonDetailsEntriesComparator does not compare null")
        }

        val o1i = PRIORITY_LIST.indexOf(o1)
        val o2i = PRIORITY_LIST.indexOf(o2)
        if(o1i == -1 && o2i == -1) return o1.compareTo(o2)
        if(o1i != -1 && o2i != -1) return o1i - o2i
        return if(o1i != -1) -1 else 1
    }
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

    private fun getHistoryCacheEntry(url: String): Maybe<HistoryItem> {
        if(history == null) throw HistoryNotInitializedError()
        return history!!.getHistoryCacheEntry(url)
    }

    fun removePeopleHistoryEntry(url: String) {
        if(history == null) throw HistoryNotInitializedError()
        history!!.delete(url)
    }

    @SuppressLint("CheckResult")
    fun getPerson(url: String): Observable<OrderedPersonDetails> {
        if(history == null) throw HistoryNotInitializedError()

        val ret: BehaviorSubject<OrderedPersonDetails> = BehaviorSubject.create()

        getHistoryCacheEntry(url).observeOn(Schedulers.io()).subscribeOn(Schedulers.computation())
            .map { gson.fromJson(it.fullSerialized, OrderedPersonDetails::class.java) }
            .toObservable()
            .switchIfEmpty {
                val sw = swapi.getPerson(url)
                sw.lastElement().observeOn(Schedulers.io()).subscribeBy { ordered ->
                    val historyItem = HistoryItem.fromOrderedPersonalDetails(ordered)
                    history!!.insert(historyItem)
                }
                sw.subscribeWith(it)
            }.subscribeWith(ret)

        return ret
    }
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
