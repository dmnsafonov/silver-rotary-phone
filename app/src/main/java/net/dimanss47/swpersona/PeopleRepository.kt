package net.dimanss47.swpersona

import androidx.paging.DataSource
import com.google.gson.JsonElement
import io.reactivex.Observable


typealias PersonDetails = Map<String, JsonElement>

data class Person(
    val name: String,
    val gender: String,
    val birthYear: String,
    val url: String
)

object PeopleRepository {
    private val swapi = SwApi.create()

    fun getPeopleList(searchTerm: String): DataSource.Factory<Int, Person> =
        swapi.getPeopleList(searchTerm)

    fun getPeopleHistoryList(): DataSource.Factory<Int, Person> {
        TODO()
    }

    fun getPerson(url: String): Observable<PersonDetails> {
        TODO()
    }
}
