package com.mapbox.navigation.examples.core

import android.content.Context
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

class HistoryUploadWorker(
    context: Context,
    private val workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val filePath = workerParams.inputData.getString(HISTORY_FILE_PATH)!!
        val sessionId = workerParams.inputData.getString(SESSION_ID)!!
        val startedAt = workerParams.inputData.getString(STARTED_AT)!!
        val userId = workerParams.inputData.getString(USER_ID)!!
        val endedAt = workerParams.inputData.getString(ENDED_AT)!!
        val driveMode = workerParams.inputData.getString(DRIVE_MODE)!!
        val appVersion = workerParams.inputData.getString(APP_VERSION)!!
        val appMode = workerParams.inputData.getString(APP_MODE)!!
        val navSdkVersion = workerParams.inputData.getString(NAV_SDK_VERSION)!!
        val navNativeSdkVersion = workerParams.inputData.getString(NAV_NATIVE_SDK_VERSION)!!
        val appSessionId = workerParams.inputData.getString(APP_SESSION_ID)!!
        val drive = Drive(
            sessionId,
            startedAt,
            userId,
            endedAt,
            filePath,
            driveMode,
            appVersion,
            appMode,
            navSdkVersion,
            navNativeSdkVersion,
            appSessionId,
        )
        if (sendToRealtimeDatabase(drive)) {
            Result.success()
        } else {
            if (runAttemptCount >= MAX_RUN_ATTEMPT_COUNT) {
                Result.failure()
            } else {
                Result.retry()
            }
        }
    }

    private fun dummyNetworkRequest(callback: Callback) {
        Thread {
            Thread.sleep(3_000)
            callback.onSuccess(true)
        }.start()
    }

    private suspend fun sendToRealtimeDatabase(drive: Drive): Boolean =
        suspendCancellableCoroutine { cont ->
            dummyNetworkRequest { cont.resume(true) }
        }

    private fun interface Callback {

        fun onSuccess(successful: Boolean)
    }

    companion object {

        private const val HISTORY_FILE_PATH: String = "history_file_path"
        private const val SESSION_ID: String = "session_id"
        private const val STARTED_AT: String = "started_at"
        private const val USER_ID: String = "user_id"
        private const val ENDED_AT: String = "ended_at"
        private const val DRIVE_MODE: String = "drive_mode"
        private const val APP_VERSION: String = "app_version"
        private const val APP_MODE: String = "app_mode"
        private const val NAV_SDK_VERSION: String = "nav_sdk_version"
        private const val NAV_NATIVE_SDK_VERSION: String = "nav_native_sdk_version"
        private const val APP_SESSION_ID: String = "app_session_id"
        private const val MAX_RUN_ATTEMPT_COUNT = 3

        fun uploadHistory(context: Context, drive: Drive) {
            val inputData = Data.Builder()
                .putString(HISTORY_FILE_PATH, drive.historyStoragePath)
                .putString(SESSION_ID, drive.sessionId)
                .putString(STARTED_AT, drive.startedAt)
                .putString(USER_ID, drive.userId)
                .putString(ENDED_AT, drive.endedAt)
                .putString(DRIVE_MODE, drive.driveMode)
                .putString(APP_VERSION, drive.appVersion)
                .putString(APP_MODE, drive.appMode)
                .putString(NAV_SDK_VERSION, drive.navSdkVersion)
                .putString(NAV_NATIVE_SDK_VERSION, drive.navNativeSdkVersion)
                .putString(APP_SESSION_ID, drive.appSessionId)
                .build()
            val uploadToFirebaseRequest = OneTimeWorkRequestBuilder<HistoryUploadWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()
                )
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context)
                .beginWith(uploadToFirebaseRequest)
                .enqueue()
        }
    }
}
