package net.dimanss47.swpersona

import android.annotation.SuppressLint
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import io.reactivex.Single
import io.reactivex.rxkotlin.subscribeBy
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import java.net.MalformedURLException
import java.net.URL

data class PersonList(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: ArrayList<Person>
)

interface SwApi {
    @GET("people/")
    fun getPeopleListRaw(
        @Query("search") searchTerm: String,
        @Query("page") page: Int
    ): Single<PersonList>

    @GET("people/{id}")
    fun getPerson(@Path("id") id: Int): Single<PersonDetails>

    companion object {
        fun create(): SwApi {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create())
                .addRxOnIoToMainThread()
                .baseUrl("https://swapi.co/api/")
                .build()
            return retrofit.create(SwApi::class.java)
        }
    }
}

fun SwApi.getPeopleList(searchTerm: String): DataSource.Factory<Int, Person> =
    SwApiPersonListDataSourceFactory { page -> getPeopleListRaw(searchTerm, page) }

class SwApiPersonListDataSourceFactory(
    private val querier: (page: Int) -> Single<PersonList>
) : DataSource.Factory<Int, Person>() {
    override fun create(): DataSource<Int, Person> =
        SwApiPersonListDataSource(querier)
}

class SwApiPersonListDataSource(
    private val querier: (page: Int) -> Single<PersonList>
) : PageKeyedDataSource<Int, Person>() {
    @SuppressLint("CheckResult")
    override fun loadInitial(
        params: LoadInitialParams<Int>,
        callback: LoadInitialCallback<Int, Person>
    ) {
        querier(1).subscribeBy(
            onError = this::forwardNetworkError,
            onSuccess = { list ->
                callback.onResult(
                    list.results,
                    0,
                    list.count,
                    getPageFromURL(list.previous),
                    getPageFromURL(list.next)
                )
            }
        )
    }

    @SuppressLint("CheckResult")
    override fun loadBefore(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, Person>
    ) {
        querier(params.key).subscribeBy(
            onError = this::forwardNetworkError,
            onSuccess = { list ->
                callback.onResult(list.results, getPageFromURL(list.previous))
            }
        )
    }

    @SuppressLint("CheckResult")
    override fun loadAfter(
        params: LoadParams<Int>,
        callback: LoadCallback<Int, Person>
    ) {
        querier(params.key).subscribeBy(
            onError = this::forwardNetworkError,
            onSuccess = { list ->
                callback.onResult(list.results, getPageFromURL(list.next))
            }
        )
    }

    private fun forwardNetworkError(e: Throwable) = NetworkErrorChannel.get().onNext(e)

    private fun getPageFromURL(urlString: String?): Int? {
        if(urlString == null) return null
        val pageParameter = try {
            URL(urlString).query
                .split('&')
                .map { it.split('=') }
                .filter { it[0] == "page" }
        } catch(e: MalformedURLException) {
            return null
        }
        val pageStr = pageParameter.getOrNull(0)?.get(1)
        return pageStr?.toIntOrNull()
    }
}

