package com.mapbox.navigation.examples.history

import android.annotation.SuppressLint
import android.content.Intent
import android.os.Bundle
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.mapbox.navigation.core.replay.history.ReplayHistoryDTO
import com.mapbox.navigation.examples.R
import com.mapbox.navigation.examples.core.ReplayHistoryActivity
import kotlinx.android.synthetic.main.history_files_activity.*

@SuppressLint("HardwareIds")
class HistoryFilesActivity : AppCompatActivity() {

    companion object {
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
                Snackbar.make(recyclerView,
                    getString(R.string.history_failed_to_load_item),
                    Snackbar.LENGTH_LONG).setAction("Action", null
                ).show()
            } else {
                selectedHistory = historyDataResponse
                launchReplayHistory()
            }
        }

        requestFileList()
        fab.setOnClickListener { requestFileList() }
    }

    override fun onBackPressed() {
        super.onBackPressed()

        launchReplayHistory()
    }

    private fun requestFileList() {
        fab.visibility = GONE
        filesViewController.requestHistoryFiles(this) { connected ->
            if (!connected) {
                Snackbar.make(recyclerView,
                    getString(R.string.history_failed_to_load_list),
                    Snackbar.LENGTH_LONG).setAction("Action", null
                ).show()
                fab.visibility = VISIBLE
            }
        }
    }

    private fun launchReplayHistory() {
        // startActivityForResult needs to destroy the calling activity to ensure mapbox
        // components are destroyed. Use startActivity to control the Activity lifecycles.
        // Note that if you want to call this from another Activity, you should consider
        // creating parameters with the Intent.
        val activityIntent = Intent(this, ReplayHistoryActivity::class.java)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        activityIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
        startActivity(activityIntent)
        finish()
    }
}
