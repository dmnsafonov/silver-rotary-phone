package net.dimanss47.swpersona

import android.app.Application
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.paging.PagedList
import androidx.paging.PagedListAdapter
import androidx.paging.RxPagedListBuilder
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import io.reactivex.Observable
import io.reactivex.disposables.Disposable
import io.reactivex.rxkotlin.subscribeBy
import kotlinx.android.synthetic.main.fragment_search.*


class SearchFragment : Fragment() {
    lateinit var viewModel: SearchViewModel
    lateinit var searchAdapterSubscription: Disposable

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
            onError = {
                TODO()
            },
            onNext = newAdapter::submitList
        )

        with(matches_list) {
            layoutManager = LinearLayoutManager(activity!!)
            adapter = newAdapter
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchAdapterSubscription.dispose()
    }

    private inner class SearchListAdapter : PagedListAdapter<Person, SearchListViewHolder>(SearchListDiffCallbacks) {
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SearchListViewHolder =
            SearchListViewHolder(layoutInflater.inflate(R.layout.person_list_item, parent, false))

        override fun onBindViewHolder(holder: SearchListViewHolder, position: Int) {
            val item = getItem(position)
            if (item == null) {
                holder.setInProgress()
            } else {
                holder.bindModel(item)
            }
        }
    }

    private class SearchListViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val name: TextView = view.findViewById(R.id.name)
        val birthYear: TextView = view.findViewById(R.id.birth_year)
        val gender: TextView = view.findViewById(R.id.gender)

        fun bindModel(person: Person) {
            name.text = person.name
            birthYear.text = person.birthYear
            gender.text = person.gender
        }

        fun setInProgress() {
            // TODO: better
            name.text = ""
            birthYear.text = ""
            gender.text = ""
        }
    }

    private object SearchListDiffCallbacks : DiffUtil.ItemCallback<Person>() {
        override fun areContentsTheSame(oldItem: Person, newItem: Person): Boolean =
            oldItem == newItem

        override fun areItemsTheSame(oldItem: Person, newItem: Person): Boolean =
            oldItem.url == newItem.url
    }
}

class SearchViewModel(app: Application) : AndroidViewModel(app) {
    var personList: Observable<PagedList<Person>> =
        RxPagedListBuilder(PeopleRepository.getPeopleList(""), PEOPLE_ON_PAGE)
            .buildObservable()

    companion object {
        private const val PEOPLE_ON_PAGE: Int = 10

        fun of(activity: FragmentActivity): SearchViewModel =
            androidx.lifecycle.ViewModelProviders
                .of(activity, ViewModelProvider.AndroidViewModelFactory(activity.application))
                .get(SearchViewModel::class.java)
    }
}
