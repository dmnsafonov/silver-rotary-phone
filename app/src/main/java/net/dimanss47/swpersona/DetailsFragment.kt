package net.dimanss47.swpersona

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_details.*


class DetailsFragment : Fragment() {
    private lateinit var viewModel: DetailsViewModel
    private var detailsAdapterSubscription: Disposable? = null
    private var networkErrorSubscription: Disposable? = null
    private var optionsMenuInitializedSubscription: Disposable? = null
    private var snackbar: Snackbar? = null

    private val activity: MainActivity?
        get() = super.getActivity() as? MainActivity

    var personUrl: String?
        get() = viewModel.url
        set(value) { viewModel.url = value }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = DetailsViewModel.of(activity!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_details, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        details_view.layoutManager = LinearLayoutManager(activity!!)

        if(!viewModel.isInErrorState) {
            val urlArg = arguments?.getString(URL_ARGUMENT_KEY)
            if(urlArg != null) viewModel.url = urlArg

            details_view.adapter = makeAdapter()
        }
    }

    private fun makeAdapter(): DetailsViewAdapter {
        val newAdapter = DetailsViewAdapter()
        detailsAdapterSubscription = viewModel.details.observeOn(AndroidSchedulers.mainThread()).subscribeBy(
            onError = NetworkErrorChannel.get()::onNext,
            onNext = newAdapter::submit
        )
        return newAdapter
    }

    override fun onStart() {
        super.onStart()

        if(networkErrorSubscription == null) {
            networkErrorSubscription = NetworkErrorChannel.get().subscribe { error ->
                viewModel.onNetworkError(error)
                makeErrorSnackbar()
            }
        }

        if(viewModel.isInErrorState) makeErrorSnackbar()

        optionsMenuInitializedSubscription = activity!!.optionsMenuInitialized
            .subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                activity!!.searchViewItem.value?.collapseActionView()

                optionsMenuInitializedSubscription?.dispose()
                optionsMenuInitializedSubscription = null
            }
    }

    override fun onStop() {
        super.onStop()

        snackbar?.dismiss()
        snackbar = null

        networkErrorSubscription?.dispose()
        networkErrorSubscription = null
    }

    override fun onDestroyView() {
        super.onDestroyView()

        detailsAdapterSubscription?.dispose()
        details_view.adapter = null
    }

    private fun makeErrorSnackbar() {
        val sb = Snackbar.make(this.view!!, R.string.network_error_message, Snackbar.LENGTH_INDEFINITE)
        sb.setAction(R.string.retry_on_network_error_action_label) {
            viewModel.onNetworkErrorResolved()
            sb.dismiss()
        }
        sb.show()
        snackbar = sb
    }

    private inner class DetailsViewAdapter : RecyclerView.Adapter<DetailsViewViewHolder>() {
        var details: List<Pair<String, PersonalDetail>>? = null

        override fun getItemCount(): Int {
            return details?.size ?: 0
        }

        override fun onBindViewHolder(holder: DetailsViewViewHolder, position: Int) {
            val (name, more) = details!![position]
            holder.bindModel(name, more)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DetailsViewViewHolder {
            return when(viewType) {
                TYPE_STRING -> DetailsViewViewHolderString(
                    layoutInflater.inflate(R.layout.detail_string, parent, false)
                )
                TYPE_ARRAY -> DetailsViewViewHolderArray(
                    layoutInflater.inflate(R.layout.detail_array, parent, false)
                )
                else -> throw IllegalStateException("unknown view type")
            }
        }

        override fun getItemViewType(position: Int): Int {
            return if(details!![position].second.isString) TYPE_STRING else TYPE_ARRAY
        }

        fun submit(newDetails: OrderedPersonDetails) {
            details = newDetails.asSequence()
                .filter { (key, _) -> key != "url" }
                .map { (key, value) -> Pair(key, value) }
                .toList()
            notifyDataSetChanged()
        }
    }

    private abstract class DetailsViewViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        abstract fun bindModel(name: String, content: PersonalDetail)
    }

    private class DetailsViewViewHolderString(view: View) : DetailsViewViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.name)
        val contentText: TextView = view.findViewById(R.id.detail_content)

        override fun bindModel(name: String, content: PersonalDetail) {
            nameText.text = name
            contentText.text = content.getString()
        }
    }

    private class DetailsViewViewHolderArray(view: View) : DetailsViewViewHolder(view) {
        val nameText: TextView = view.findViewById(R.id.name)
//        val contentView: RecyclerView = view.findViewById(R.id.detail_content)
        val contentText: TextView = view.findViewById(R.id.detail_content)

        override fun bindModel(name: String, content: PersonalDetail) {
            nameText.text = name
            contentText.text = content.getContents()
                .filter { (_, ready) -> ready }
                .joinToString("\n") { (content, _) -> content }
            // TODO: use recyclerview with layout and more data
        }
    }

    companion object {
        const val URL_ARGUMENT_KEY: String = "person_url"

        const val TYPE_STRING: Int = 1
        const val TYPE_ARRAY: Int = 2
    }
}

class DetailsViewModel : ViewModel() {
    var url: String? = null
        set(value) {
            val changed = field != value
            field = value
            if(changed) reset()
        }

    private var subscription: Disposable? = null
    private var detailsRaw: Observable<OrderedPersonDetails>? = null
    private val detailsImpl: BehaviorSubject<OrderedPersonDetails> = BehaviorSubject.create()

    val details: Observable<OrderedPersonDetails> = detailsImpl

    private fun reset() {
        subscription?.dispose()
        if(url != null) {
            detailsRaw = makeDetails()
            subscription = makeSubscription()
        }
    }

    private fun makeDetails(): Observable<OrderedPersonDetails> = PeopleRepository.getPerson(url!!)

    private fun makeSubscription(): Disposable = detailsRaw!!.subscribeWith(
        object : DisposableObserver<OrderedPersonDetails>() {
            override fun onComplete() {}
            override fun onError(e: Throwable) = detailsImpl.onError(e)
            override fun onNext(t: OrderedPersonDetails) = detailsImpl.onNext(t)
        }
    )

    var isInErrorState: Boolean = false
        private set

    fun onNetworkError(e: Throwable) {
        // TODO: different messages, network availability detection
        isInErrorState = true
    }

    fun onNetworkErrorResolved() {
        isInErrorState = false
        reset()
    }

    companion object {
        fun of(activity: FragmentActivity): DetailsViewModel =
            androidx.lifecycle.ViewModelProviders
                .of(activity)
                .get(DetailsViewModel::class.java)
    }
}
