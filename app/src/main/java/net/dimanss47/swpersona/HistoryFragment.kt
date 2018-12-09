package net.dimanss47.swpersona

import android.app.Application
import androidx.fragment.app.Fragment
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.paging.RxPagedListBuilder
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ItemTouchHelper
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.Disposable
import io.reactivex.schedulers.Schedulers
import kotlinx.android.synthetic.main.fragment_history.*


class HistoryFragment : Fragment() {
    private lateinit var viewModel: HistoryViewModel
    private var historyListSubscription: Disposable? = null

    private val activity: MainActivity?
        get() = super.getActivity() as? MainActivity

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = HistoryViewModel.of(activity!!)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_history, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val newAdapter = HistoryListAdapter()
        historyListSubscription = viewModel.history.subscribe(newAdapter::submitList)
        with(history_view) {
            layoutManager = LinearLayoutManager(activity!!)
            adapter = newAdapter
        }
        ItemTouchHelper(ItemTouchCallback(newAdapter)).attachToRecyclerView(history_view)
        history_view.addOnItemTouchListener(PersonListOnItemTouchListener(activity!!, history_view))
    }

    override fun onDestroyView() {
        super.onDestroyView()
        historyListSubscription?.dispose()
        historyListSubscription = null
    }

    private inner class HistoryListAdapter
            : PagedListAdapter<HistoryItem, PersonListViewHolder>(HistoryItemDiffCallback) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PersonListViewHolder =
            PersonListViewHolder(layoutInflater.inflate(R.layout.person_list_item, parent, false))

        override fun onBindViewHolder(holder: PersonListViewHolder, position: Int) {
            val item = getItem(position)
            if(item == null) {
                holder.setInProgress()
            } else {
                holder.bindModel(item)
            }
        }
    }

    private object HistoryItemDiffCallback : DiffUtil.ItemCallback<HistoryItem>() {
        override fun areContentsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean =
            oldItem == newItem

        override fun areItemsTheSame(oldItem: HistoryItem, newItem: HistoryItem): Boolean =
            oldItem.url == newItem.url
    }

    private class ItemTouchCallback(private val adapter: HistoryListAdapter) : ItemTouchHelper.SimpleCallback(
        0,
        ItemTouchHelper.START or ItemTouchHelper.END
    ) {
        override fun isLongPressDragEnabled(): Boolean = false

        override fun onMove(
            recyclerView: RecyclerView,
            viewHolder: RecyclerView.ViewHolder,
            target: RecyclerView.ViewHolder
        ): Boolean = throw IllegalStateException("must not be called")

        override fun onSwiped(viewHolder: RecyclerView.ViewHolder, direction: Int) {
            val url = (viewHolder as PersonListViewHolder).url

            // prevent deleting not yet loaded items
            if(url == null) {
                adapter.notifyItemChanged(viewHolder.adapterPosition)
                return
            }

            Schedulers.io().scheduleDirect {
                PeopleRepository.removePeopleHistoryEntry(url)
            }
        }
    }
}

class HistoryViewModel(app: Application) : AndroidViewModel(app) {
    val history: Observable<PagedList<HistoryItem>>

    init {
        with(PeopleRepository) {
            init(app)
            history = RxPagedListBuilder(PeopleRepository.getPeopleHistoryList(), PEOPLE_ON_PAGE)
                .buildObservable().observeOn(AndroidSchedulers.mainThread())
        }
    }

    companion object {
        private const val PEOPLE_ON_PAGE: Int = 10

        fun of(activity: FragmentActivity): HistoryViewModel =
            androidx.lifecycle.ViewModelProviders
                .of(activity)
                .get(HistoryViewModel::class.java)
    }
}
