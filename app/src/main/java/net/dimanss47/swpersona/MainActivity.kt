package net.dimanss47.swpersona

import android.content.res.Configuration
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.fragment.app.Fragment
import androidx.fragment.app.transaction

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    companion object {
        const val HISTORY_FRAGMENT_TAG: String = "history_fragment"
        const val SEARCH_FRAGMENT_TAG: String = "search_fragment"
        const val DETAILS_FRAGMENT_TAG: String = "details_fragment"
    }

    var returnToHistory = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        replaceLeftFragment({ HistoryFragment() }, HISTORY_FRAGMENT_TAG)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        initSearchWidget()
        return true
    }

    private fun initSearchWidget() {
        with(searchView.value) {
            isSubmitButtonEnabled = false
            setIconifiedByDefault(false)
        }

        searchViewItem.value.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                replaceLeftFragment(::SearchFragment, SEARCH_FRAGMENT_TAG)
                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if(returnToHistory) replaceLeftFragment(::HistoryFragment, HISTORY_FRAGMENT_TAG)
                return true
            }
        })
    }

    private fun replaceLeftFragment(getFragment: () -> Fragment, tag: String) {
        val fragment = supportFragmentManager.findFragmentByTag(tag) ?: getFragment()
        supportFragmentManager.transaction {
            replace(R.id.left_pane, fragment)
        }
    }

    fun openDetails(url: String) {
        if(orientation == Configuration.ORIENTATION_LANDSCAPE) {
            detailsFragment!!.personUrl = url
            returnToHistory = true
        } else {
            val fragment = detailsFragment ?: DetailsFragment()

            val args = Bundle()
            args.putString(DetailsFragment.URL_ARGUMENT_KEY, url)
            fragment.arguments = args

            supportFragmentManager.transaction {
                replace(R.id.left_pane, fragment, DETAILS_FRAGMENT_TAG)
            }

            returnToHistory = false
        }
    }

    val searchViewItem
        get() = lazy {
            toolbar.menu.findItem(R.id.action_search)
        }

    val searchView
        get() = lazy {
            searchViewItem.value.actionView as SearchView
        }

    val orientation: Int
        get() = resources.configuration.orientation

    val detailsFragment
        get() = supportFragmentManager.findFragmentByTag(DETAILS_FRAGMENT_TAG) as DetailsFragment?
}
