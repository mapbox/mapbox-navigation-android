package com.mapbox.navigation.examples.core.replay

import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.google.gson.stream.JsonReader
import com.mapbox.navigation.core.replay.history.ReplayEventStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.InputStream
import java.io.InputStreamReader

class HistoryFileLoader {
    suspend fun loadReplayHistory(
        context: Context
    ): ReplayEventStream = withContext(Dispatchers.IO) {
        HistoryFilesActivity.selectedReplay
            ?: loadHistoryJsonFromAssets(context)
    }

    private fun loadHistoryJsonFromAssets(context: Context): ReplayEventStream {
        val fileName = "replay-history-activity.json"
        val inputStream: InputStream = context.assets.open(fileName)
        val jsonReader = JsonReader(InputStreamReader(inputStream))
        FirebaseCrashlytics.getInstance().log("loadHistoryJsonFromAssets")
        return ReplayEventStream(jsonReader)
    }
}
