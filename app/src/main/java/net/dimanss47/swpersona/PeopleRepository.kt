package net.dimanss47.swpersona

import androidx.paging.DataSource
import com.google.gson.annotations.SerializedName
import io.reactivex.Observable
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

    fun getPeopleList(searchTerm: String): DataSource.Factory<String?, Person> =
        swapi.getPeopleList(searchTerm)

    fun getPeopleHistoryList(): DataSource.Factory<Int, Person> {
        TODO()
    }

    fun getPerson(url: String): Observable<OrderedPersonDetails> =
        swapi.getPerson(url)
}
