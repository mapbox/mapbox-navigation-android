package com.mapbox.navigation.copilot.work

import android.content.Context
import androidx.annotation.VisibleForTesting
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.mapbox.common.MapboxOptions
import com.mapbox.common.TransferState
import com.mapbox.common.UploadOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.copilot.AttachmentMetadata
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.attachmentFilename
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.generateSessionId
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.rename
import com.mapbox.navigation.copilot.HttpServiceProvider
import com.mapbox.navigation.copilot.MapboxCopilot
import com.mapbox.navigation.copilot.MapboxCopilot.pushStatusObservers
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.GZ
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.LOG_CATEGORY
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.MEDIA_TYPE_ZIP
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.PROD_BASE_URL
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.ZIP
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.gson
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.copilot.internal.PushStatus
import com.mapbox.navigation.copilot.internal.saveFilename
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
        val copilotSession = copilotSessionFrom(workerParams.inputData)
        val recordingFile =
            rename(File(copilotSession.recording), attachmentFilename(copilotSession))
        val sessionFile = File(recordingFile.parent, copilotSession.saveFilename())
        val uploadSessionId = workerParams.inputData.getString(UPLOAD_SESSION_ID)!!

        val metadataList = arrayListOf(
            AttachmentMetadata(
                name = recordingFile.name,
                created = copilotSession.startedAt,
                fileId = "",
                format = GZ,
                type = ZIP,
                sessionId = uploadSessionId,
            ),
        )
        val uploadOptions = UploadOptions(
            /* filePath = */ recordingFile.absolutePath,
            /* url = */ workerParams.inputData.getString(UPLOAD_URL)!!,
            /* headers = */ HashMap(),
            /* metadata = */ gson.toJson(metadataList),
            /* mediaType = */ MEDIA_TYPE_ZIP,
            /* sdkInformation = */ MapboxCopilot.sdkInformation,
        )

        if (uploadHistoryFile(uploadOptions)) {
            success(copilotSession)
            logD("Result.success(${recordingFile.name}|${sessionFile.name})")
            delete(recordingFile)
            delete(sessionFile)
            Result.success()
        } else {
            failure(copilotSession)
            if (runAttemptCount >= MAX_RUN_ATTEMPT_COUNT) {
                logD("Result.failure()")
                delete(recordingFile)
                delete(sessionFile)
                Result.failure()
            } else {
                logD("Result.retry()")
                Result.retry()
            }
        }
    }

    private fun delete(file: File) {
        logD("Deleting ${file.name}")
        HistoryAttachmentsUtils.delete(file)
    }

    private suspend fun uploadHistoryFile(
        uploadOptions: UploadOptions,
    ): Boolean = suspendCancellableCoroutine { cont ->
        logD("start history file upload")

        val uploadService = HttpServiceProvider.getInstance()
        val uploadId = uploadService.upload(uploadOptions) { uploadStatus ->
            when (uploadStatus.state) {
                TransferState.PENDING -> {
                    logD("uploadStatus state = PENDING")
                }

                TransferState.IN_PROGRESS -> {
                    logD(
                        "uploadStatus state = UPLOADING sent ${uploadStatus.totalSentBytes}" +
                            "/${uploadStatus.totalBytes} bytes",
                    )
                }

                TransferState.FINISHED -> {
                    val httpResultCode = uploadStatus.httpResult?.value?.code
                    logD("uploadStatus state = FINISHED httpResultCode = $httpResultCode")
                    cont.resume(httpResultCode.isSuccessful())
                }

                TransferState.FAILED -> {
                    logD(
                        "uploadStatus state = FAILED error = ${uploadStatus.error}; " +
                            "HttpResponseData = ${uploadStatus.httpResult?.value}",
                    )
                    cont.resume(false)
                }
            }
        }
        cont.invokeOnCancellation { throwable ->
            uploadService.cancelUpload(uploadId) {
                logD("cancel upload due to ${throwable?.message}")
            }
        }
    }

    private fun failure(copilotSession: CopilotSession) {
        val failedStatus = PushStatus.Failed(copilotSession)
        pushStatusObservers.forEach {
            it.onPushStatusChanged(failedStatus)
        }
    }

    private fun Int?.isSuccessful(): Boolean = this in 200..299

    private fun success(drive: CopilotSession) {
        val successStatus = PushStatus.Success(drive)
        pushStatusObservers.forEach {
            it.onPushStatusChanged(successStatus)
        }
    }

    private fun logD(msg: String) = logD("[upload] $msg", LOG_CATEGORY)

    internal companion object {

        private const val HISTORY_FILE_PATH = "history_file_path"
        private const val APP_MODE = "app_mode"
        private const val DRIVE_MODE = "drive_mode"
        private const val DRIVE_ID = "drive_id"
        private const val STARTED_AT = "started_at"
        private const val ENDED_AT = "ended_at"
        private const val NAV_SDK_VERSION = "nav_sdk_version"
        private const val NAV_NATIVE_SDK_VERSION = "nav_native_sdk_version"
        private const val APP_VERSION = "app_version"
        private const val APP_USER_ID = "app_user_id"
        private const val APP_SESSION_ID = "app_session_id"
        private const val UPLOAD_URL = "upload_url"
        private const val UPLOAD_SESSION_ID = "upload_session_id"
        private const val OWNER = "owner"

        // 2^8 x 338 = 86528 / 3600 = 24.03 hours
        private const val MAX_RUN_ATTEMPT_COUNT = 8
        private const val DELAY_IN_SECONDS = 338L

        /**
         * uploadHistory
         */
        fun uploadHistory(
            context: Context,
            copilotSession: CopilotSession,
        ) {
            val workName = "copilot-upload.${copilotSession.recording}"
            val workRequest = OneTimeWorkRequestBuilder<HistoryUploadWorker>()
                .setConstraints(requireInternet())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, DELAY_IN_SECONDS, TimeUnit.SECONDS)
                .setInputData(inputData(copilotSession))
                .addTag("copilot-upload")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(workName, ExistingWorkPolicy.KEEP, workRequest)
        }

        private fun requireInternet() =
            Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()

        private fun inputData(copilotSession: CopilotSession): Data {
            val url = "$PROD_BASE_URL/attachments/v1?access_token=${MapboxOptions.accessToken}"
            val uploadSessionId = generateSessionId(copilotSession)

            return Data.Builder()
                .putCopilotSession(copilotSession)
                .putString(UPLOAD_URL, url)
                .putString(UPLOAD_SESSION_ID, uploadSessionId)
                .build()
        }

        private fun copilotSessionFrom(data: Data): CopilotSession = CopilotSession(
            appMode = data.getString(APP_MODE)!!,
            driveMode = data.getString(DRIVE_MODE)!!,
            driveId = data.getString(DRIVE_ID)!!,
            startedAt = data.getString(STARTED_AT)!!,
            endedAt = data.getString(ENDED_AT)!!,
            navSdkVersion = data.getString(NAV_SDK_VERSION)!!,
            navNativeSdkVersion = data.getString(NAV_NATIVE_SDK_VERSION)!!,
            appVersion = data.getString(APP_VERSION)!!,
            appUserId = data.getString(APP_USER_ID)!!,
            appSessionId = data.getString(APP_SESSION_ID)!!,
            recording = data.getString(HISTORY_FILE_PATH)!!,
            owner = data.getString(OWNER).orEmpty(),
        )

        @VisibleForTesting
        internal fun Data.Builder.putCopilotSession(copilotSession: CopilotSession): Data.Builder =
            putString(APP_MODE, copilotSession.appMode)
                .putString(DRIVE_MODE, copilotSession.driveMode)
                .putString(DRIVE_ID, copilotSession.driveId)
                .putString(STARTED_AT, copilotSession.startedAt)
                .putString(ENDED_AT, copilotSession.endedAt)
                .putString(NAV_SDK_VERSION, copilotSession.navSdkVersion)
                .putString(NAV_NATIVE_SDK_VERSION, copilotSession.navNativeSdkVersion)
                .putString(APP_USER_ID, copilotSession.appUserId)
                .putString(APP_VERSION, copilotSession.appVersion)
                .putString(APP_SESSION_ID, copilotSession.appSessionId)
                .putString(HISTORY_FILE_PATH, copilotSession.recording)
                .putString(OWNER, copilotSession.owner)
    }
}
