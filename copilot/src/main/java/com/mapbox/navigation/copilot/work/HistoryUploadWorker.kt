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
import androidx.work.WorkRequest
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
import com.mapbox.navigation.copilot.MapboxCopilotImpl.Companion.reportCopilotError
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.copilot.internal.PushStatus
import com.mapbox.navigation.copilot.internal.saveFilename
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logW
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
     *
     * TODO(NAVAND-5879) worker can be stopped if it runs longer than 10 minutes
     *
     * A ListenableWorker is given a maximum of ten minutes to finish its execution and return a
     * Result. After this time has expired, the worker will be signalled to stop and its
     * com.google.common.util.concurrent.ListenableFuture will be cancelled.
     */
    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val copilotSession = copilotSessionFrom(workerParams.inputData)
        val recordingFile =
            rename(File(copilotSession.recording), attachmentFilename(copilotSession))
        val sessionFile = File(recordingFile.parent, copilotSession.saveFilename())

        if (!recordingFile.exists()) {
            reportCopilotError(
                "History file does not exist: ${recordingFile.name}. " +
                    "Copilot session: $copilotSession. " +
                    "Copilot dir files: ${recordingFile.parentFile?.listFiles()?.map { it.name }}",
            )
        }

        if (!sessionFile.exists()) {
            reportCopilotError(
                "Session file does not exist: ${sessionFile.name}. " +
                    "Copilot session: $copilotSession. " +
                    "Copilot dir files: ${recordingFile.parentFile?.listFiles()?.map { it.name }}",
            )
        }

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
                logW("Result.failure(${recordingFile.name}|${sessionFile.name})")
                delete(recordingFile)
                delete(sessionFile)
                Result.failure()
            } else {
                logD(
                    "Result.retry(" +
                        "${recordingFile.name}|${sessionFile.name}, " +
                        "runAttemptCount = $runAttemptCount" +
                        ")",
                )
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

                    uploadStatus.httpResult?.onError { error ->
                        reportCopilotError(
                            "History upload failed for ${uploadOptions.filePath}. " +
                                "Response error = $error",
                        )
                    }?.onValue { value ->
                        if (!value.code.isSuccessful()) {
                            reportCopilotError(
                                "History upload failed for ${uploadOptions.filePath}. " +
                                    "Response data = $value",
                            )
                        }
                    }

                    cont.resume(httpResultCode.isSuccessful())
                }

                TransferState.FAILED -> {
                    logW(
                        "uploadStatus state = FAILED error = ${uploadStatus.error}; " +
                            "HttpResponseData = ${uploadStatus.httpResult?.value}",
                    )

                    reportCopilotError(
                        "History upload failed for ${uploadOptions.filePath}. " +
                            "Error = ${uploadStatus.error}, " +
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

    private fun logD(msg: String) = logD("[upload] [$id] $msg", LOG_CATEGORY)
    private fun logW(msg: String) = logW("[upload] [$id] $msg", LOG_CATEGORY)

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

        /**
         * Max backoff delay is limited by [WorkRequest.MAX_BACKOFF_MILLIS] (5 hours = 18 000 seconds).
         * The total cumulative retry time could be approximately 20 hours
         * (it can be more depending on system delays):
         *
         * 300 * 2⁰	= 300
         * 300 * 2¹	= 600
         * 300 * 2²	= 1200
         * 300 * 2³	= 2400
         * 300 * 2⁴	= 4800
         * 300 * 2⁵	= 9600
         * 300 * 2⁶	= 19200 -> capped to 18000
         * 300 * 2⁷	= 38400 -> capped to 18000
         * 300 × 2⁸	= 76800 -> caped to 18000
         *
         * Total delay is 72900 seconds ≈ 20 hours
         */
        const val MAX_RUN_ATTEMPT_COUNT = 9
        private const val DELAY_IN_SECONDS = 300L // 5 minutes

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
