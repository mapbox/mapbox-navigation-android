package com.mapbox.navigation.copilot

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mapbox.common.TransferState
import com.mapbox.common.UploadOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.copilot.MapboxCopilot.pushStatusObservers
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.GZ
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.LOG_CATEGORY
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.MEDIA_TYPE_ZIP
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.ZIP
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.gson
import com.mapbox.navigation.copilot.internal.CopilotMetadata
import com.mapbox.navigation.copilot.internal.PushStatus
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume

/**
 * HistoryUploadWorker
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class HistoryUploadWorker(
    context: Context,
    private val workerParams: WorkerParameters,
) : CoroutineWorker(context, workerParams) {

    /**
     * doWork
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val drive = buildNavigationSessionFrom(workerParams.inputData)
        val filePath = workerParams.inputData.getString(HISTORY_FILE_PATH)!!
        val file = File(filePath)
        val startedAt = workerParams.inputData.getString(STARTED_AT)!!
        val uploadSessionId = workerParams.inputData.getString(UPLOAD_SESSION_ID)!!
        val metadata = AttachmentMetadata(
            name = file.name,
            created = startedAt,
            fileId = "",
            format = GZ,
            type = ZIP,
            sessionId = uploadSessionId,
        )
        val metadataList = arrayListOf(metadata)
        val uploadUrl = workerParams.inputData.getString(UPLOAD_URL)!!
        val uploadOptions = UploadOptions(
            file.absolutePath,
            uploadUrl,
            HashMap(),
            gson.toJson(metadataList),
            MEDIA_TYPE_ZIP,
            MapboxCopilot.sdkInformation,
        )
        if (uploadHistoryFile(drive, uploadOptions)) {
            logD("Result.success()", LOG_CATEGORY)
            HistoryAttachmentsUtils.delete(file)
            Result.success()
        } else {
            if (runAttemptCount >= MAX_RUN_ATTEMPT_COUNT) {
                logD("Result.failure()", LOG_CATEGORY)
                HistoryAttachmentsUtils.delete(file)
                Result.failure()
            } else {
                logD("Result.retry()", LOG_CATEGORY)
                Result.retry()
            }
        }
    }

    private fun buildNavigationSessionFrom(data: Data): CopilotMetadata {
        val appMode = data.getString(APP_MODE)!!
        val driveMode = data.getString(DRIVE_MODE)!!
        val driveId = data.getString(DRIVE_ID)!!
        val startedAt = data.getString(STARTED_AT)!!
        val endedAt = data.getString(ENDED_AT)!!
        val navSdkVersion = data.getString(NAV_SDK_VERSION)!!
        val navNativeSdkVersion = data.getString(NAV_NATIVE_SDK_VERSION)!!
        val appVersion = data.getString(APP_VERSION)!!
        val appUserId = data.getString(APP_USER_ID)!!
        val appSessionId = data.getString(APP_SESSION_ID)!!
        return CopilotMetadata(
            appMode,
            driveMode,
            driveId,
            startedAt,
            endedAt,
            navSdkVersion,
            navNativeSdkVersion,
            appVersion,
            appUserId,
            appSessionId,
        )
    }

    private suspend fun uploadHistoryFile(
        drive: CopilotMetadata,
        uploadOptions: UploadOptions,
    ): Boolean =
        suspendCancellableCoroutine { cont ->
            val uploadService = HttpServiceProvider.getInstance()
            val uploadId = uploadService.upload(uploadOptions) { uploadStatus ->
                when (uploadStatus.state) {
                    TransferState.PENDING -> logD("uploadStatus state = PENDING", LOG_CATEGORY)
                    TransferState.IN_PROGRESS -> {
                        logD("uploadStatus state = UPLOADING", LOG_CATEGORY)
                        logD("${uploadStatus.totalSentBytes} total sent bytes", LOG_CATEGORY)
                        logD("${uploadStatus.totalBytes} total bytes", LOG_CATEGORY)
                    }
                    TransferState.FAILED -> {
                        logD(
                            "uploadStatus state = FAILED error = ${uploadStatus.error} " +
                                "HttpResponseData = ${uploadStatus.httpResult?.value}",
                            LOG_CATEGORY,
                        )
                        failure(drive)
                        cont.resume(false)
                    }
                    TransferState.FINISHED -> {
                        val httpResultCode = uploadStatus.httpResult?.value?.code
                        logD(
                            "uploadStatus state = FINISHED httpResultCode = $httpResultCode",
                            LOG_CATEGORY,
                        )
                        if (httpResultCode.isSuccessful()) {
                            success(drive)
                            cont.resume(true)
                        } else {
                            failure(drive)
                            cont.resume(false)
                        }
                    }
                }
            }
            cont.invokeOnCancellation { throwable ->
                uploadService.cancelUpload(uploadId) {
                    logD("cancel upload due to ${throwable?.message}", LOG_CATEGORY)
                }
            }
        }

    private fun failure(drive: CopilotMetadata) {
        val failedStatus = PushStatus.Failed(drive)
        pushStatusObservers.forEach {
            it.onPushStatusChanged(failedStatus)
        }
    }

    private fun Int?.isSuccessful(): Boolean = this in 200..299

    private fun success(drive: CopilotMetadata) {
        val successStatus = PushStatus.Success(drive)
        pushStatusObservers.forEach {
            it.onPushStatusChanged(successStatus)
        }
    }

    internal companion object {

        private const val HISTORY_FILE_PATH: String = "history_file_path"
        private const val APP_MODE: String = "app_mode"
        private const val DRIVE_MODE: String = "drive_mode"
        private const val DRIVE_ID: String = "drive_id"
        private const val STARTED_AT: String = "started_at"
        private const val ENDED_AT: String = "ended_at"
        private const val NAV_SDK_VERSION: String = "nav_sdk_version"
        private const val NAV_NATIVE_SDK_VERSION: String = "nav_native_sdk_version"
        private const val APP_VERSION: String = "app_version"
        private const val APP_USER_ID: String = "app_user_id"
        private const val APP_SESSION_ID: String = "app_session_id"
        private const val UPLOAD_URL: String = "upload_url"
        private const val UPLOAD_SESSION_ID: String = "upload_session_id"

        // 2^8 x 338 = 86528 / 3600 = 24.03 hours
        private const val MAX_RUN_ATTEMPT_COUNT = 8
        private const val DELAY_IN_SECONDS = 338L

        /**
         * uploadHistory
         */
        fun uploadHistory(
            context: Context,
            drive: CopilotMetadata,
            uploadOptions: UploadOptions,
            sessionId: String,
        ) {
            val inputData = buildInputData(drive, uploadOptions, sessionId)
            val uploadServiceRequest = OneTimeWorkRequestBuilder<HistoryUploadWorker>()
                .setConstraints(
                    Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build(),
                )
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, DELAY_IN_SECONDS, TimeUnit.SECONDS)
                .setInputData(inputData)
                .build()

            WorkManager.getInstance(context)
                .beginWith(uploadServiceRequest)
                .enqueue()
        }

        private fun buildInputData(
            drive: CopilotMetadata,
            uploadOptions: UploadOptions,
            sessionId: String,
        ): Data =
            Data.Builder()
                .putString(HISTORY_FILE_PATH, uploadOptions.filePath)
                .putString(APP_MODE, drive.appMode)
                .putString(DRIVE_MODE, drive.driveMode)
                .putString(DRIVE_ID, drive.driveId)
                .putString(STARTED_AT, drive.startedAt)
                .putString(ENDED_AT, drive.endedAt)
                .putString(NAV_SDK_VERSION, drive.navSdkVersion)
                .putString(NAV_NATIVE_SDK_VERSION, drive.navNativeSdkVersion)
                .putString(APP_USER_ID, drive.appUserId)
                .putString(APP_VERSION, drive.appVersion)
                .putString(APP_SESSION_ID, drive.appSessionId)
                .putString(UPLOAD_URL, uploadOptions.url)
                .putString(UPLOAD_SESSION_ID, sessionId)
                .build()
    }
}
