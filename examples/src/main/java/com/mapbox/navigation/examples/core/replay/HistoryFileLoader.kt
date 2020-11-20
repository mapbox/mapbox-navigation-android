package com.mapbox.navigation.examples.core.replay

import android.annotation.SuppressLint
import android.content.Context
import com.mapbox.navigation.core.replay.history.ReplayHistoryEventStream
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryFileLoader {
    private val historyFilesDirectory = HistoryFilesDirectory()

    @SuppressLint("MissingPermission")
    suspend fun loadReplayHistory(
        context: Context
    ): ReplayHistoryEventStream = withContext(Dispatchers.IO) {
        HistoryFilesActivity.selectedHistory ?: loadDefaultReplayHistory(context)
    }

    private suspend fun loadDefaultReplayHistory(
        context: Context
    ): ReplayHistoryEventStream = withContext(Dispatchers.IO) {
        val fileName = "replay-history-activity.json"
        val inputStream = context.assets.open(fileName)
        val outputFile = historyFilesDirectory.outputFile(context, fileName)
        outputFile.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
        ReplayHistoryEventStream(outputFile.absolutePath)
    }
}
