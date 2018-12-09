package net.dimanss47.swpersona

import android.annotation.SuppressLint
import androidx.paging.DataSource
import androidx.paging.PageKeyedDataSource
import com.google.gson.*
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import io.reactivex.subjects.BehaviorSubject
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url
import java.util.*

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

    @GET
    fun getPersonRaw(@Url personUrl: String): Single<PersonDetailsRaw>

    @GET
    fun getPersonalDetail(@Url detailUrl: String): Single<ExternalPersonalDetail>

    companion object {
        fun create(): SwApi {
            val retrofit = Retrofit.Builder()
                .addConverterFactory(GsonConverterFactory.create(gson))
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

@SuppressLint("CheckResult")
fun SwApi.getPerson(personUrl: String): Observable<OrderedPersonDetails> {
    val ret = BehaviorSubject.create<OrderedPersonDetails>()

    ret.onNext(OrderedPersonDetails(TreeMap()))
    getPersonRaw(personUrl).observeOn(Schedulers.computation()).subscribeBy(
        onError = {
            ret.onComplete()
            forwardNetworkError(it)
        },
        onSuccess = { unordered ->
            val ordered = unordered.toOrdered()
            val contentObservables = ArrayList<Single<Triple<String, Int, ExternalPersonalDetail>>>()

            ordered.asSequence()
                .filter { (_, detail) -> detail.isContents }
                .flatMapTo(contentObservables) { entry ->
                    entry.value.getUrls().asSequence()
                        .mapIndexed { i, (url, _) -> Triple(entry.key, i, url) }
                        .map { (name, i, url) ->
                            getPersonalDetail(url).map { Triple(name, i, it) }
                        }
                }

            ret.onNext(ordered.copy())

            Single.merge(contentObservables)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeBy(
                onError = ret::onError,
                onComplete = ret::onComplete,
                onNext = { (name, ind, detail) ->
                    val contents = ordered[name]!!.getContents()
                    contents[ind] = Pair(detail.name!!, true)
                    ret.onNext(ordered.copy())
                }
            )
        }
    )

    return ret
}

class SwApiListDataSourceFactory<E>(
    private val querier: (page: String?) -> Single<SwApiList<E>>
) : DataSource.Factory<String?, E>() {
    override fun create(): DataSource<String?, E> =
        SwApiListDataSource(querier)
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
            onError = ::forwardNetworkError,
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
            onError = ::forwardNetworkError,
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
            onError = ::forwardNetworkError,
            onSuccess = { list: SwApiList<E> ->
                callback.onResult(list.results, list.next)
            }
        )
    }
}

class PersonDetailsRaw(
    private val data: Map<String, JsonElement>
) : Map<String, JsonElement> by data {

    fun toOrdered(): OrderedPersonDetails {
        val ret = TreeMap<String, PersonalDetail>(PersonDetailsEntriesComparator)
        data.asSequence()
            .filter { (_, value) -> !value.isJsonNull }
            .associateTo(ret) { (name, value) ->
                if(value.isJsonPrimitive) {
                    if(name == "homeworld") {
                        return@associateTo Pair(
                            name,
                            PersonalDetail.contents(mutableListOf(Pair(value.asString, false)))
                        )
                    }

                    return@associateTo Pair(
                        name,
                        PersonalDetail.string(value.asString)
                    )
                }

                val objects: Iterable<JsonElement> =
                    when(value) {
                        is JsonObject -> listOf(value)
                        is JsonArray -> value
                        else -> throw IllegalStateException("unknown json element type")
                    }

                val urlList = objects.asSequence().map {
                    if(it !is JsonPrimitive) throw JsonParseError("expected json primitive")
                    if(!it.isString) throw JsonParseError("expected url string")
                    it.asString
            }.toMutableList()

            return@associateTo Pair(name, PersonalDetail.urls(urlList))
        }

        return OrderedPersonDetails(ret)
    }
}

class JsonParseError(message: String?) : Throwable(message)

class PersonalDetail private constructor(
    private val mString: String? = null,
    private val mContents: MutableList<Pair<String, Boolean>>? = null
) {
    val isString
        get() = mString != null
    val isContents
        get() = mContents != null

    fun getString() = mString!!
    fun getUrls() = mContents!!.filter { (_, isContents) -> !isContents }
    fun getContents() = mContents!!

    companion object {
        fun string(str: String) = PersonalDetail(mString = str)
        fun urls(urls: List<String>) =
            PersonalDetail(mContents = Collections.synchronizedList(urls.map { Pair(it, false) }))
        fun contents(contents: List<Pair<String, Boolean>>) =
            PersonalDetail(mContents = Collections.synchronizedList(contents))
    }
}

private fun forwardNetworkError(e: Throwable) {
    NetworkErrorChannel.get().onNext(e)
}
