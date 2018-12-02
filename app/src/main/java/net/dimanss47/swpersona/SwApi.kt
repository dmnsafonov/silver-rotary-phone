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
import retrofit2.http.Url

data class PersonList(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: ArrayList<Person>
)

interface SwApi {
    @GET("people/")
    fun getPeopleListRaw(@Query("search") searchTerm: String): Single<PersonList>

    @GET
    fun getPeopleListPage(@Url pageUrl: String): Single<PersonList>

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

fun SwApi.getPeopleList(searchTerm: String): DataSource.Factory<String?, Person> =
    SwApiPersonListDataSourceFactory { page ->
        if(page == null) {
            getPeopleListRaw(searchTerm)
        } else {
            getPeopleListPage(page)
        }
    }

class SwApiPersonListDataSourceFactory(
    private val querier: (page: String?) -> Single<PersonList>
) : DataSource.Factory<String?, Person>() {
    override fun create(): DataSource<String?, Person> =
        SwApiPersonListDataSource(querier)
}

class SwApiPersonListDataSource(
    private val querier: (page: String?) -> Single<PersonList>
) : PageKeyedDataSource<String?, Person>() {
    @SuppressLint("CheckResult")
    override fun loadInitial(
        params: LoadInitialParams<String?>,
        callback: LoadInitialCallback<String?, Person>
    ) {
        querier(null).subscribeBy(
            onError = this::forwardNetworkError,
            onSuccess = { list ->
                callback.onResult(
                    list.results,
                    0,
                    list.count,
                    list.previous,
                    list.next
                )
            }
        )
    }

    @SuppressLint("CheckResult")
    override fun loadBefore(
        params: LoadParams<String?>,
        callback: LoadCallback<String?, Person>
    ) {
        querier(params.key).subscribeBy(
            onError = this::forwardNetworkError,
            onSuccess = { list ->
                callback.onResult(list.results, list.previous)
            }
        )
    }

    @SuppressLint("CheckResult")
    override fun loadAfter(
        params: LoadParams<String?>,
        callback: LoadCallback<String?, Person>
    ) {
        querier(params.key).subscribeBy(
            onError = this::forwardNetworkError,
            onSuccess = { list ->
                callback.onResult(list.results, list.next)
            }
        )
    }

    private fun forwardNetworkError(e: Throwable) = NetworkErrorChannel.get().onNext(e)
}

