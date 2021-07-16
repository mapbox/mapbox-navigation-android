package com.mapbox.navigation.examples.core.replay

import android.annotation.SuppressLint
import android.content.Context
import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.replay.history.ReplayEventBase
import com.mapbox.navigation.core.replay.history.ReplayHistoryMapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class HistoryFileLoader {
    private val replayHistoryMapper = ReplayHistoryMapper.Builder().build()
    private val historyFilesDirectory = HistoryFilesDirectory()

    @SuppressLint("MissingPermission")
    suspend fun loadReplayHistory(
        context: Context
    ): List<ReplayEventBase> = withContext(Dispatchers.IO) {
        loadSelectedHistory() ?: loadDefaultReplayHistory(context)
    }

    private suspend fun loadSelectedHistory(): List<ReplayEventBase>? =
        withContext(Dispatchers.IO) {
            HistoryFilesActivity.selectedHistory?.asSequence()?.mapNotNull { historyEvent ->
                replayHistoryMapper.mapToReplayEvent(historyEvent)
            }?.toList()
        }

    private suspend fun loadDefaultReplayHistory(
        context: Context
    ): List<ReplayEventBase> = withContext(Dispatchers.IO) {
        val fileName = "replay-history-activity.json"
        val inputStream = context.assets.open(fileName)
        val outputFile = historyFilesDirectory.outputFile(context, fileName)
        outputFile.outputStream().use { fileOut ->
            inputStream.copyTo(fileOut)
        }
        MapboxHistoryReader(outputFile.absolutePath)
            .asSequence()
            .mapNotNull { replayHistoryMapper.mapToReplayEvent(it) }
            .toList()
    }
}
