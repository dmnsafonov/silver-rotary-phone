package net.dimanss47.swpersona

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.transaction
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.subjects.BehaviorSubject

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    private var detailsFragment: DetailsFragment? = null
    private var transitionDoNotBack = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        val url = savedInstanceState?.getString(URL_STATE_KEY)
        if(url == null) {
            addFragment(R.id.left_pane, ::HistoryFragment)
        } else {
            addFragment(R.id.left_pane, ::HistoryFragment, { createDetailsWithUrl(url) })
        }

        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            addFragment(R.id.right_pane, ::DetailsFragment)
        }
    }

    private fun addFragment(id: Int, first: (() -> Fragment)?, vararg getFragment: () -> Fragment) {
        if(first != null) {
            supportFragmentManager.transaction {
                replace(id, first())
            }
        }
        getFragment.forEach {
            supportFragmentManager.transaction {
                addToBackStack(null)
                replace(id, it())
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        initSearchWidget()
        optionsMenuInitializedImpl.onNext(true)
        return true
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        if(detailsFragment != null) {
            outState.putString(URL_STATE_KEY, detailsFragment!!.personUrl)
        }
    }

    private fun initSearchWidget() {
        with(searchView.value!!) {
            isSubmitButtonEnabled = false
            setIconifiedByDefault(false)
        }

        searchViewItem.value!!.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                // TODO: disable when is in search
                addFragment(R.id.left_pane, null, ::SearchFragment)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if(!transitionDoNotBack) supportFragmentManager.popBackStack()
                transitionDoNotBack = false
                return true
            }
        })
    }

    fun openDetails(url: String) {
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            detailsFragment!!.personUrl = url
        } else supportFragmentManager.transaction {
            transitionDoNotBack = true
            addFragment(R.id.left_pane, null, { createDetailsWithUrl(url) })
        }
    }

    private fun createDetailsWithUrl(url: String): Fragment {
        val fragment = DetailsFragment()

        val args = Bundle()
        args.putString(DetailsFragment.URL_ARGUMENT_KEY, url)
        fragment.arguments = args

        return fragment
    }

    fun onDetailsFragmentAttached(fragment: DetailsFragment) {
        detailsFragment = fragment
    }

    fun onDetailsFragmentDetached() {
        detailsFragment = null
    }

    val searchViewItem: Lazy<MenuItem?>
        get() = lazy {
            toolbar.menu.findItem(R.id.action_search)
        }

    val searchView: Lazy<SearchView?>
        get() = lazy {
            searchViewItem.value?.actionView as SearchView?
        }

    private val optionsMenuInitializedImpl: BehaviorSubject<Boolean> = BehaviorSubject.create()
    val optionsMenuInitialized: Observable<Boolean> =
        optionsMenuInitializedImpl.observeOn(AndroidSchedulers.mainThread())

    val orientation: Int
        get() = resources.configuration.orientation

    companion object {
        const val URL_STATE_KEY: String = "details_url"
    }
}
