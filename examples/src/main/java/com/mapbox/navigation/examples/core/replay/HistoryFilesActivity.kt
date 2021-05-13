package com.mapbox.navigation.examples.core.replay

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.google.android.material.snackbar.Snackbar
import com.mapbox.navigation.core.replay.history.ReplayEventStream
import com.mapbox.navigation.examples.core.R

@SuppressLint("HardwareIds")
class HistoryFilesActivity : AppCompatActivity() {

    companion object {
        val REQUEST_CODE: Int = 123
        var selectedReplay: ReplayEventStream? = null
            private set
    }

    private lateinit var filesViewController: HistoryFilesViewController
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView
    private lateinit var fab: FloatingActionButton

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_files_activity)
        toolbar = findViewById(R.id.toolbar)
        recyclerView = findViewById(R.id.recyclerView)
        fab = findViewById(R.id.fab)

        setSupportActionBar(toolbar)

        val viewManager = LinearLayoutManager(recyclerView.context)
        val viewAdapter = HistoryFileAdapter()
        recyclerView.apply {
            setHasFixedSize(true)
            layoutManager = viewManager
            adapter = viewAdapter
        }

        filesViewController = HistoryFilesViewController()
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
                selectedReplay = historyDataResponse
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
