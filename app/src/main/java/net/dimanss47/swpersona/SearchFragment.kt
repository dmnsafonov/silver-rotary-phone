package net.dimanss47.swpersona

import android.os.Bundle
import android.view.*
import androidx.fragment.app.Fragment
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModel
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.paging.RxPagedListBuilder
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.snackbar.Snackbar
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.observers.DisposableObserver
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.subjects.BehaviorSubject
import kotlinx.android.synthetic.main.fragment_search.*


class SearchFragment : Fragment() {
    private lateinit var viewModel: SearchViewModel
    private lateinit var searchAdapterSubscription: Disposable
    private var networkErrorSubscription: Disposable? = null
    private var optionsMenuInitializedSubscription: Disposable? = null
    private var snackbar: Snackbar? = null

    private val activity: MainActivity?
        get() = super.getActivity() as? MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = SearchViewModel.of(activity!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_search, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val newAdapter = SearchListAdapter()
        searchAdapterSubscription = viewModel.personList.subscribeBy(
            onError = NetworkErrorChannel.get()::onNext,
            onNext = newAdapter::submitList
        )

        with(matches_list) {
            layoutManager = LinearLayoutManager(activity!!)
            adapter = newAdapter
        }

        matches_list.addOnItemTouchListener(object : RecyclerView.SimpleOnItemTouchListener() {
            val gestureDetector = GestureDetector(
                activity!!.applicationContext,
                object : GestureDetector.SimpleOnGestureListener() {
                    override fun onSingleTapUp(e: MotionEvent): Boolean = true
                }
            )

            init { gestureDetector.setIsLongpressEnabled(false) }

            override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
                gestureDetector.onTouchEvent(e)
            }

            // TODO: ripple, "selected" color
            override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
                val childView = matches_list.findChildViewUnder(e.x, e.y)
                    ?: return false
                val viewHolder = matches_list.getChildViewHolder(childView)
                if(!gestureDetector.onTouchEvent(e)) return false
                val url = (viewHolder as PersonListViewHolder).url
                    ?: return false
                activity!!.openDetailsFromSearch(url)
                return true
            }
        })
    }

    override fun onStart() {
        super.onStart()

        if(networkErrorSubscription == null) {
            networkErrorSubscription = NetworkErrorChannel.get().subscribe { error ->
                viewModel.onNetworkError(error)
                makeErrorSnackbar()
            }
        }

        optionsMenuInitializedSubscription = activity!!.optionsMenuInitialized
            .subscribeOn(AndroidSchedulers.mainThread()).subscribe {
                activity!!.searchView.value!!.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
                    override fun onQueryTextChange(newText: String): Boolean {
                        viewModel.personNameFilter = newText
                        return true
                    }

                    override fun onQueryTextSubmit(query: String): Boolean = true
                })

                optionsMenuInitializedSubscription?.dispose()
                optionsMenuInitializedSubscription = null
            }

        if(viewModel.isInErrorState) makeErrorSnackbar()
    }

    override fun onStop() {
        super.onStop()

        activity!!.searchView.value!!.setOnQueryTextListener(null)

        snackbar?.dismiss()
        snackbar = null

        networkErrorSubscription?.dispose()
        networkErrorSubscription = null
    }

    override fun onDestroyView() {
        super.onDestroyView()

        searchAdapterSubscription.dispose()
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

    private inner class SearchListAdapter : PagedListAdapter<Person, PersonListViewHolder>(SearchListDiffCallbacks) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonListViewHolder =
            PersonListViewHolder(layoutInflater.inflate(R.layout.person_list_item, parent, false))

        override fun onBindViewHolder(holder: PersonListViewHolder, position: Int) {
            val item = getItem(position)
            if (item == null) {
                holder.setInProgress()
            } else {
                holder.bindModel(item)
            }
        }
    }

    private object SearchListDiffCallbacks : DiffUtil.ItemCallback<Person>() {
        override fun areContentsTheSame(oldItem: Person, newItem: Person): Boolean =
            oldItem == newItem

        override fun areItemsTheSame(oldItem: Person, newItem: Person): Boolean =
            oldItem.url == newItem.url
    }
}

class SearchViewModel : ViewModel() {
    var personNameFilter: String = ""
        set(value) {
            val changed = field != value
            field = value
            if(changed) reset()
        }

    private var personListRaw = makePersonList()
    private var personListImpl: BehaviorSubject<PagedList<Person>> = BehaviorSubject.create()
    private var personListRawSubscription: Disposable = makePersonListSubscription()

    val personList: Observable<PagedList<Person>> = personListImpl

    private fun reset() {
        personListRawSubscription.dispose()
        personListRaw = makePersonList()
        personListRawSubscription = makePersonListSubscription()
    }

    private fun makePersonList(): Observable<PagedList<Person>> =
        RxPagedListBuilder(PeopleRepository.getPeopleList(personNameFilter), PEOPLE_ON_PAGE)
            .buildObservable()

    private fun makePersonListSubscription(): Disposable = personListRaw.subscribeWith(
        object : DisposableObserver<PagedList<Person>>() {
            override fun onComplete() {}
            override fun onError(e: Throwable) = personListImpl.onError(e)
            override fun onNext(t: PagedList<Person>) = personListImpl.onNext(t)
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
        private const val PEOPLE_ON_PAGE: Int = 10

        fun of(activity: FragmentActivity): SearchViewModel =
            androidx.lifecycle.ViewModelProviders
                .of(activity)
                .get(SearchViewModel::class.java)
    }
}
