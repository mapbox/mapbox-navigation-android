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
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.create
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
import com.mapbox.navigation.utils.internal.logE
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
    private val context: Context,
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
        val sessionFile = create(recordingFile.parent, copilotSession.saveFilename())

        logD(
            "HistoryUploadWorker.doWork(). " +
                "Session: $copilotSession, " +
                "runAttemptCount: $runAttemptCount",
        )

        if (copilotSession.endedAt.isEmpty()) {
            reportCopilotError(
                "Passed copilot session has empty endedAt date: $copilotSession, " +
                    "Copilot dir files: ${recordingFile.parentFile?.listFiles()?.map { it.name }}",
            )
        }

        if (!recordingFile.exists()) {
            reportCopilotError(
                "History file does not exist: ${recordingFile.name}. " +
                    "Copilot session: $copilotSession. " +
                    "Copilot dir files: ${recordingFile.parentFile?.listFiles()?.map { it.name }}",
            )
            cleanup(copilotSession, recordingFile, sessionFile)
            return@withContext Result.failure()
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
            cleanup(copilotSession, recordingFile, sessionFile)
            Result.success()
        } else {
            failure(copilotSession)
            if (runAttemptCount >= MAX_RUN_ATTEMPT_COUNT) {
                logW("Result.failure(${recordingFile.name}|${sessionFile.name})")
                cleanup(copilotSession, recordingFile, sessionFile)
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

    private fun cleanup(
        copilotSession: CopilotSession,
        recordingFile: File,
        sessionFile: File,
    ) {
        if (!delete(sessionFile)) {
            reportCopilotError("Can't delete session file")
        }

        if (!delete(recordingFile)) {
            reportCopilotError("Can't delete recording file")
        }

        /**
         * Cancels any pending upload jobs that may have been scheduled by
         * [PeriodicHistoryCleanupWorker].
         *
         * NOTE: TODO This does not fully guarantee that a new job won’t be scheduled if
         * [PeriodicHistoryCleanupWorker] is currently running.
         */
        cancelScheduledUploading(context, copilotSession)
    }

    private fun delete(file: File): Boolean {
        logD("Deleting ${file.name}")
        return try {
            HistoryAttachmentsUtils.delete(file)
        } catch (e: SecurityException) {
            logE("Deleting ${file.name} error: $e")
            false
        }
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
    private fun logE(msg: String) = logE("[upload] [$id] $msg", LOG_CATEGORY)

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
         * With [MAX_RUN_ATTEMPT_COUNT] = 15, there are 15 total attempts (1 initial + 14 retries).
         * The total cumulative retry time could be approximately 20 hours
         * (it can be more depending on system delays):
         *
         * Attempt 1: runs immediately (no delay)
         * Delay before attempt 2: 10 * 2⁰	= 10
         * Delay before attempt 3: 10 * 2¹	= 20
         * Delay before attempt 4: 10 * 2²	= 40
         * Delay before attempt 5: 10 * 2³	= 80
         * Delay before attempt 6: 10 * 2⁴	= 160
         * Delay before attempt 7: 10 * 2⁵	= 320
         * Delay before attempt 8: 10 * 2⁶	= 640
         * Delay before attempt 9: 10 * 2⁷	= 1280
         * Delay before attempt 10: 10 * 2⁸	= 2560
         * Delay before attempt 11: 10 * 2⁹	= 5120
         * Delay before attempt 12: 10 * 2¹⁰ = 10240
         * Delay before attempt 13: 10 * 2¹¹ = 20480 -> capped to 18000
         * Delay before attempt 14: 10 * 2¹² = 40960 -> capped to 18000
         * Delay before attempt 15: 10 * 2¹³ = 81920 -> capped to 18000
         *
         * Total cumulative delay is 74470 seconds ≈ 20 hours
         */
        const val MAX_RUN_ATTEMPT_COUNT = 15
        private const val DELAY_IN_SECONDS = 10L

        /**
         * uploadHistory
         */
        fun uploadHistory(
            context: Context,
            copilotSession: CopilotSession,
        ) {
            val workRequest = OneTimeWorkRequestBuilder<HistoryUploadWorker>()
                .setConstraints(requireInternet())
                .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, DELAY_IN_SECONDS, TimeUnit.SECONDS)
                .setInputData(inputData(copilotSession))
                .addTag("copilot-upload")
                .build()

            WorkManager.getInstance(context)
                .enqueueUniqueWork(
                    copilotSession.workName(),
                    ExistingWorkPolicy.KEEP,
                    workRequest,
                )
        }

        fun cancelScheduledUploading(context: Context, copilotSession: CopilotSession) {
            WorkManager.getInstance(context)
                .cancelUniqueWork(copilotSession.workName())
        }

        private fun CopilotSession.workName() = "copilot-upload.$recording"

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
