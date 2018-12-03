package net.dimanss47.swpersona

import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.processors.FlowableProcessor
import io.reactivex.processors.PublishProcessor
import io.reactivex.rxkotlin.subscribeBy
import org.reactivestreams.Processor

class NetworkErrorChannel private constructor(
    private val processor: FlowableProcessor<Throwable>
) : Processor<Throwable, Throwable> by processor {
    fun subscribe(onNext: (Throwable) -> Unit): Disposable =
        processor.observeOn(AndroidSchedulers.mainThread()).subscribeBy(onNext = onNext)

    companion object {
        private val INSTANCE: NetworkErrorChannel by lazy { NetworkErrorChannel(PublishProcessor.create()) }
        fun get(): NetworkErrorChannel = INSTANCE
    }
}
