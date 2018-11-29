package net.dimanss47.swpersona

import android.os.Bundle
import android.view.Menu
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView

import kotlinx.android.synthetic.main.activity_main.*

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        setSupportActionBar(toolbar)
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
    }

    val searchViewItem
        get() = lazy {
            toolbar.menu.findItem(R.id.action_search)
        }

    val searchView
        get() = lazy {
            searchViewItem.value.actionView as SearchView
        }
}
