package com.mapbox.navigation.examples.history

import android.annotation.SuppressLint
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mapbox.navigation.core.replay.history.ReplayHistoryDTO
import com.mapbox.navigation.examples.R
import kotlinx.android.synthetic.main.history_files_activity.*

@SuppressLint("HardwareIds")
class HistoryFilesActivity : AppCompatActivity() {

    companion object {
        val REQUEST_CODE: Int = 123
        var selectedHistory: ReplayHistoryDTO? = null
            private set
    }

    private lateinit var filesViewController: HistoryFilesViewController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.history_files_activity)
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
