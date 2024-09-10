package com.mapbox.navigation.copilot.internal

import android.content.Context
import android.content.pm.ApplicationInfo
import com.google.gson.Gson
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.EventsAppMetadata
import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.retrieveNavNativeSdkVersion
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.retrieveNavSdkVersion
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.utcTimeNow
import java.io.File
import java.util.Locale
import kotlin.io.path.Path
import kotlin.io.path.nameWithoutExtension

/**
 * Copilot session info.
 *
 * @property appMode mode of the application / environment
 * @property driveMode either active-guidance or free-drive
 * @property driveId unique identifier of a single drive (_Active Guidance_ or _Free Drive_)
 * @property startedAt UTC timestamp in yyyy-mm-ddThh:MM:ss.msZ format
 * @property endedAt UTC timestamp in yyyy-mm-ddThh:MM:ss.msZ format
 * @property navSdkVersion Navigation SDK version used
 * @property navNativeSdkVersion NN version used
 * @property appVersion of the application
 * @property appUserId [EventsAppMetadata] user identifier (optional). See [NavigationOptions]
 * @property appSessionId [EventsAppMetadata] session identifier (optional). See [NavigationOptions]
 * @property recording file path to which the history recording was written
 */
data class CopilotSession(
    val appMode: String = "",
    val driveMode: String = "",
    val driveId: String = "_",
    val startedAt: String = "",
    val endedAt: String = "",
    val navSdkVersion: String = retrieveNavSdkVersion(),
    val navNativeSdkVersion: String = retrieveNavNativeSdkVersion(),
    val appVersion: String = "",
    val appUserId: String = "_",
    val appSessionId: String = "_",
    val recording: String = "",
) {
    fun toJson(): String = Gson().toJson(this)

    companion object {

        fun fromJson(json: String): Result<CopilotSession> = runCatching {
            Gson().fromJson(json, CopilotSession::class.java)
        }

        @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
        fun create(
            navigationOptions: NavigationOptions,
            driveId: String,
            driveMode: String,
            recording: String,
        ) = with(navigationOptions) {
            CopilotSession(
                appMode = if (applicationContext.isAppDebuggable()) "mbx-debug" else "mbx-prod",
                driveMode = driveMode,
                driveId = driveId,
                startedAt = currentUtcTime(),
                endedAt = "",
                navSdkVersion = retrieveNavSdkVersion(),
                navNativeSdkVersion = retrieveNavNativeSdkVersion(),
                appVersion = applicationContext.getVersionName(),
                appUserId = copilotOptions.userId ?: eventsAppMetadata?.userId ?: "_",
                appSessionId = eventsAppMetadata?.sessionId ?: "_",
                recording = recording,
            )
        }
    }
}

private fun Context.getVersionName() =
    packageManager.getPackageInfo(packageName, 0).versionName

private fun Context.isAppDebuggable(): Boolean =
    applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0

internal fun currentUtcTime(
    format: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
    locale: Locale = Locale.US,
): String = utcTimeNow(format, locale)

internal fun CopilotSession.saveFilename(): String {
    val name = Path(recording).nameWithoutExtension.substringBefore(".")
    return if (name.isNotBlank()) "$name.metadata.json" else "$startedAt.metadata.json"
}

internal fun File.listCopilotSessionFiles(): Array<File> =
    listFiles { _, filename -> filename.endsWith("metadata.json") } ?: emptyArray()

internal fun File.listCopilotRecordingFiles(): Array<File> =
    listFiles { _, filename -> filename.endsWith("pbf.gz") } ?: emptyArray()
