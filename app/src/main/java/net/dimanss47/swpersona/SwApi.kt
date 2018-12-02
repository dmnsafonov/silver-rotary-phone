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

data class SwApiList<E>(
    val count: Int,
    val next: String?,
    val previous: String?,
    val results: ArrayList<E>
)

interface SwApi {
    @GET("people/")
    fun getPeopleListRaw(@Query("search") searchTerm: String): Single<SwApiList<Person>>

    @GET
    fun getPeopleListPage(@Url pageUrl: String): Single<SwApiList<Person>>

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
    SwApiListDataSourceFactory { page ->
        if(page == null) {
            getPeopleListRaw(searchTerm)
        } else {
            getPeopleListPage(page)
        }
    }

class SwApiListDataSourceFactory<E>(
    private val querier: (page: String?) -> Single<SwApiList<E>>
) : DataSource.Factory<String?, E>() {
    override fun create(): DataSource<String?, E> =
        SwApiListDataSource<E>(querier)
}

class SwApiListDataSource<E>(
    private val querier: (page: String?) -> Single<SwApiList<E>>
) : PageKeyedDataSource<String?, E>() {
    @SuppressLint("CheckResult")
    override fun loadInitial(
        params: LoadInitialParams<String?>,
        callback: LoadInitialCallback<String?, E>
    ) {
        querier(null).subscribeBy(
            onError = this::forwardNetworkError,
            onSuccess = { list: SwApiList<E> ->
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
        callback: LoadCallback<String?, E>
    ) {
        querier(params.key).subscribeBy(
            onError = this::forwardNetworkError,
            onSuccess = { list: SwApiList<E> ->
                callback.onResult(list.results, list.previous)
            }
        )
    }

    @SuppressLint("CheckResult")
    override fun loadAfter(
        params: LoadParams<String?>,
        callback: LoadCallback<String?, E>
    ) {
        querier(params.key).subscribeBy(
            onError = this::forwardNetworkError,
            onSuccess = { list: SwApiList<E> ->
                callback.onResult(list.results, list.next)
            }
        )
    }

    private fun forwardNetworkError(e: Throwable) = NetworkErrorChannel.get().onNext(e)
}

