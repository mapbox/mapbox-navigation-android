package com.mapbox.navigation.copilot.work

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.delete
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.LOG_CATEGORY
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.reportCopilotError
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.copilot.internal.CopilotSession.Companion.attachmentFile
import com.mapbox.navigation.copilot.internal.CopilotSession.Companion.recordingFile
import com.mapbox.navigation.copilot.internal.listCopilotRecordingFiles
import com.mapbox.navigation.copilot.internal.listCopilotSessionFiles
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Periodic task worker that scans HISTORY_FILES_DIR and:
 * - schedules [HistoryUploadWorker] task for each .pbf.gz recording file that has
 *   matching .metadata.json session file, except the latest one (active copilot session)
 * - deletes all residual .pbf.gz recording files that's missing .metadata.json session file
 *
 * IMPORTANT: This worker expects all files to be chronologically named.
 * It uses file name to identify latest recording file.
 */
internal class PeriodicHistoryCleanupWorker(
    context: Context,
    private val workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    private val historyFilesDir by lazy { workerParams.inputData.getString(HISTORY_FILES_DIR)!! }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val sessions = loadAllCopilotSessions()
        scheduleRecordingsUpload(nonActiveSessions = sessions.dropLast(1))
        deleteResidualRecordings(sessions)
        Result.success()
    }

    private fun loadAllCopilotSessions(): List<CopilotSession> {
        return File(historyFilesDir)
            .listCopilotSessionFiles()
            .sortedBy { it.absolutePath }
            .mapNotNull { file ->
                logD("Processing ${file.name}")
                CopilotSession.fromJson(file.readText()).getOrNull()
            }
    }

    private fun scheduleRecordingsUpload(nonActiveSessions: List<CopilotSession>) {
        return nonActiveSessions.forEach { session ->
            val recordingFile = session.recordingFile
            val file = if (recordingFile.exists()) {
                recordingFile
            } else {
                val attachmentFile = session.attachmentFile
                if (attachmentFile.exists()) {
                    attachmentFile
                } else {
                    null
                }
            }

            if (file != null) {
                logD("Uploading recording ${file.absolutePath}")
                HistoryUploadWorker.uploadHistory(applicationContext, session)
            }
        }
    }

    private fun deleteResidualRecordings(sessions: List<CopilotSession>) {
        val excludedRecordings: MutableSet<String> = mutableSetOf()

        sessions.forEach {
            excludedRecordings.add(it.recording)
            excludedRecordings.add(it.attachmentFile.absolutePath)
        }

        File(historyFilesDir)
            .listCopilotRecordingFiles()
            .sortedBy { it.absolutePath }
            .filter { it.absolutePath !in excludedRecordings }
            .forEach { fileToDelete ->
                logD("Deleting recording ${fileToDelete.name}")
                delete(fileToDelete)

                reportCopilotError(
                    "Deleting residual recording: $fileToDelete," +
                        "Copilot dir files: ${
                        fileToDelete.parentFile?.listFiles()?.map { it.name }
                        }",
                )
            }
    }

    private fun logD(msg: String) = logD("[cleanup] [$id] $msg", LOG_CATEGORY)

    internal companion object {

        private const val HISTORY_FILES_DIR: String = "history_files_dir"

        fun scheduleWork(
            context: Context,
            historyFilesDir: String?,
        ) {
            if (historyFilesDir.isNullOrBlank()) {
                logE("Failed to schedule periodic upload work! Missing $HISTORY_FILES_DIR.")
                return
            }

            val workRequest = PeriodicWorkRequestBuilder<PeriodicHistoryCleanupWorker>(
                15,
                TimeUnit.MINUTES, // repeatInterval
                5,
                TimeUnit.MINUTES, // flexTimeInterval
            ).setInputData(
                Data.Builder()
                    .putString(HISTORY_FILES_DIR, historyFilesDir)
                    .build(),
            )
                .addTag("copilot-cleanup")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniquePeriodicWork(
                    "copilot-cleanup.periodic",
                    ExistingPeriodicWorkPolicy.KEEP,
                    workRequest,
                )
        }

        private fun logE(msg: String) = logE("[cleanup] $msg", LOG_CATEGORY)
    }
}
