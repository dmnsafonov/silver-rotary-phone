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
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)

        if(isOrientationLandscape) {
            onCreateLandscape(savedInstanceState)
        } else {
            onCreatePortrait(savedInstanceState)
        }
    }

    private fun onCreateLandscape(savedInstanceState: Bundle?) {
        if(savedInstanceState == null) {
            supportFragmentManager.transaction {
                add(R.id.left_pane, HistoryFragment(), HISTORY_FRAGMENT_LANDSCAPE_TAG)
                add(R.id.right_pane, DetailsFragment(), DETAILS_FRAGMENT_LANDSCAPE_TAG)
            }
        } else {
            if(!isHistoryVisible) supportFragmentManager.transaction {
                replace(
                    R.id.left_pane,
                    getHistoryFragment() ?: HistoryFragment(),
                    HISTORY_FRAGMENT_LANDSCAPE_TAG
                )
            }

            if(!isDetailsVisible) supportFragmentManager.transaction {
                replace(
                    R.id.right_pane,
                    getDetailsFragment() ?: DetailsFragment(),
                    DETAILS_FRAGMENT_LANDSCAPE_TAG
                )
            }

            val url = savedInstanceState.getString(URL_STATE_KEY)
            if(url != null) {
                supportFragmentManager.executePendingTransactions()
                getDetailsFragment()!!.personUrl = url
            }
        }
    }

    private fun onCreatePortrait(savedInstanceState: Bundle?) {
        if(savedInstanceState == null) {
            supportFragmentManager.transaction {
                add(R.id.content_frame, HistoryFragment(), HISTORY_FRAGMENT_TAG)
            }
        } else {
            val url = savedInstanceState.getString(URL_STATE_KEY)

            if(url != null) {
                var details = getDetailsFragment()
                if(details == null) {
                    details = createDetailsWithUrl(url)
                } else {
                    details.personUrl = url
                }

                if(!isDetailsVisible) supportFragmentManager.transaction {
                    replace(R.id.content_frame, details, DETAILS_FRAGMENT_TAG)
                }
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

        // orientation is already changed when onSaveInstanceState() is called
        var details = getDetailsFragment()
        if(details?.isVisible != true) details = getDetailsFragment(reverse = true)

        if(details?.isVisible == true) {
            outState.putString(URL_STATE_KEY, details.personUrl)
        }
    }

    private fun initSearchWidget() {
        with(searchView.value!!) {
            isSubmitButtonEnabled = false
            setIconifiedByDefault(false)
        }

        searchViewItem.value!!.setOnActionExpandListener(object : MenuItem.OnActionExpandListener {
            override fun onMenuItemActionExpand(item: MenuItem): Boolean {
                if(isOrientationLandscape) supportFragmentManager.transaction {
                    replace(R.id.left_pane, SearchFragment(), SEARCH_FRAGMENT_LANDSCAPE_TAG)
                }

                if(!isOrientationLandscape) supportFragmentManager.transaction {
                    replace(R.id.content_frame, SearchFragment(), SEARCH_FRAGMENT_TAG)
                    addToBackStack(null)
                }

                return true
            }

            override fun onMenuItemActionCollapse(item: MenuItem): Boolean {
                if(isSearchVisible) {
                    if(isOrientationLandscape) supportFragmentManager.transaction {
                        replace(R.id.left_pane, HistoryFragment())
                    } else {
                        supportFragmentManager.popBackStack()
                    }
                }
                return true
            }
        })
    }

    fun openDetailsFromSearch(url: String) {
        if(isOrientationLandscape) {
            getDetailsFragment()!!.personUrl = url
        } else supportFragmentManager.transaction {
            supportFragmentManager.transaction {
                replace(R.id.content_frame, createDetailsWithUrl(url), DETAILS_FRAGMENT_TAG)
                addToBackStack(null)
            }
        }
    }

    private fun createDetailsWithUrl(url: String): DetailsFragment {
        val fragment = DetailsFragment()

        val args = Bundle()
        args.putString(DetailsFragment.URL_ARGUMENT_KEY, url)
        fragment.arguments = args

        return fragment
    }

    override fun onBackPressed() {
        if(isOrientationLandscape) {
            finish() // TODO: navigate to previous record, than toast before finishing
        } else {
            super.onBackPressed()
        }
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

    private val isOrientationLandscape: Boolean
        get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    private fun getHistoryFragment(): HistoryFragment? =
        getFragment(HISTORY_FRAGMENT_TAG, HISTORY_FRAGMENT_LANDSCAPE_TAG) as HistoryFragment?

    private fun getDetailsFragment(reverse: Boolean = false): DetailsFragment? =
        getFragment(DETAILS_FRAGMENT_TAG, DETAILS_FRAGMENT_LANDSCAPE_TAG, reverse) as DetailsFragment?

    private fun getSearchFragment(): SearchFragment? =
        getFragment(SEARCH_FRAGMENT_TAG, SEARCH_FRAGMENT_LANDSCAPE_TAG) as SearchFragment?

    private fun getFragment(portraitTag: String, landscapeTag: String, reverse: Boolean = false): Fragment? {
        var landscape = isOrientationLandscape
        if(reverse) landscape = !landscape

        return if (landscape) {
            supportFragmentManager.findFragmentByTag(landscapeTag)
        } else {
            supportFragmentManager.findFragmentByTag(portraitTag)
        }
    }

    private val isHistoryVisible: Boolean
        get() = getHistoryFragment()?.isVisible ?: false

    val isSearchVisible: Boolean
        get() = getSearchFragment()?.isVisible ?: false

    private val isDetailsVisible: Boolean
        get() = getDetailsFragment()?.isVisible ?: false

    companion object {
        const val URL_STATE_KEY = "details_url"

        const val HISTORY_FRAGMENT_TAG = "history_fragment"
        const val HISTORY_FRAGMENT_LANDSCAPE_TAG = "history_fragment_landscape"
        const val DETAILS_FRAGMENT_TAG = "details_fragment"
        const val DETAILS_FRAGMENT_LANDSCAPE_TAG = "details_fragment_landscape"
        const val SEARCH_FRAGMENT_TAG = "search_fragment"
        const val SEARCH_FRAGMENT_LANDSCAPE_TAG = "search_fragment_landscape"
    }
}
