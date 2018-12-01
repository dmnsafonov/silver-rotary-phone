package net.dimanss47.swpersona

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
    }

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
                replaceLeftFragment(::HistoryFragment, HISTORY_FRAGMENT_TAG)
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
}
