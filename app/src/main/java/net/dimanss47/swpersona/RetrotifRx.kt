// adapted from retrofit/samples/src/main/java/com/example/retrofit/RxJavaObserveOnMainThread.java

package net.dimanss47.swpersona

import io.reactivex.Observable
import io.reactivex.Scheduler
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import retrofit2.Call
import retrofit2.CallAdapter
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import java.lang.reflect.Type


fun Retrofit.Builder.addRxOnIoToMainThread(): Retrofit.Builder {
    addCallAdapterFactory(ObserveOnMainCallAdapterFactory(AndroidSchedulers.mainThread()))
    addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.io()))
    return this
}

class ObserveOnMainCallAdapterFactory(val scheduler: Scheduler) : CallAdapter.Factory() {
    override fun get(
        returnType: Type, annotations: Array<Annotation>, retrofit: Retrofit
    ): CallAdapter<*, *>? {
        if (CallAdapter.Factory.getRawType(returnType) != Observable::class.java) {
            return null
        }

        @Suppress("UNCHECKED_CAST")
        val delegate = retrofit.nextCallAdapter(
            this,
            returnType,
            annotations
        ) as CallAdapter<Any, Observable<*>>

        return object : CallAdapter<Any, Any> {
            override fun adapt(call: Call<Any>): Any {
                val o = delegate.adapt(call)
                return o.observeOn(scheduler)
            }

            override fun responseType(): Type {
                return delegate.responseType()
            }
        }
    }
}
