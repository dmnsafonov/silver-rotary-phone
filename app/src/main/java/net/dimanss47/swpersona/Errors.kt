package net.dimanss47.swpersona

import io.reactivex.disposables.Disposable
import io.reactivex.processors.BehaviorProcessor
import io.reactivex.processors.FlowableProcessor
import io.reactivex.rxkotlin.subscribeBy
import org.reactivestreams.Processor

class NetworkErrorChannel private constructor(
    private val processor: FlowableProcessor<Throwable>
) : Processor<Throwable, Throwable> by processor {
    fun subscribe(onNext: (Throwable) -> Unit): Disposable = processor.subscribeBy(onNext = onNext)

    companion object {
        private val INSTANCE: NetworkErrorChannel by lazy { NetworkErrorChannel(BehaviorProcessor.create()) }
        fun get(): NetworkErrorChannel = INSTANCE
    }
}
