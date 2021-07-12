package com.mapbox.navigation.examples.core.replay

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.appbar.CollapsingToolbarLayout
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.examples.core.R

@SuppressLint("HardwareIds")
class HistoryFilesActivity : AppCompatActivity() {

    companion object {
        const val REQUEST_CODE: Int = 123
        const val EXTRA_HISTORY_FILE_DIRECTORY = "EXTRA_HISTORY_FILE_DIRECTORY"
        var selectedHistory: MapboxHistoryReader? = null
            private set
    }

    private lateinit var filesViewController: HistoryFilesViewController
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton
    private lateinit var collapsingToolbar: CollapsingToolbarLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_files_activity)
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fab)

        collapsingToolbar = findViewById(R.id.collapsing_toolbar)
        collapsingToolbar.setExpandedTitleTextAppearance(R.style.ExpandedToolbarStyle)
        collapsingToolbar.setCollapsedTitleTextAppearance(R.style.CollapsedToolbarStyle)

        setSupportActionBar(toolbar)

        val viewManager = LinearLayoutManager(recyclerView.context)
        val viewAdapter = HistoryFileAdapter()
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        val historyFileDirectory = intent.extras?.getString(EXTRA_HISTORY_FILE_DIRECTORY)
        filesViewController = HistoryFilesViewController(historyFileDirectory)
        filesViewController.attach(this, viewAdapter) { historyDataResponse ->
            if (historyDataResponse == null) {
                Snackbar.make(
                    recyclerView,
                    getString(R.string.history_failed_to_load_item),
                    Snackbar.LENGTH_LONG
                ).setAction(
                    "Action",
                    null
                ).show()
            } else {
                selectedHistory = historyDataResponse
                finish()
            }
        }

        requestFileList()
        fab.setOnClickListener { requestFileList() }
    }

    private fun requestFileList() {
        fab.visibility = GONE
        filesViewController.requestHistoryFiles(this) { connected ->
            if (!connected) {
                Snackbar.make(
                    recyclerView,
                    getString(R.string.history_failed_to_load_list),
                    Snackbar.LENGTH_LONG
                ).setAction(
                    "Action",
                    null
                ).show()
                fab.visibility = VISIBLE
            }
        }
    }
}
