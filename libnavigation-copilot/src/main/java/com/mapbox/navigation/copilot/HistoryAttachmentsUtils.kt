package com.mapbox.navigation.copilot

import android.util.Base64
import com.mapbox.navigation.copilot.internal.CopilotMetadata
import com.mapbox.navigation.core.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

internal object HistoryAttachmentsUtils {

    private const val HISTORY_FILENAME_SEPARATOR = "__"
    private const val SDK_PLATFORM = "android"
    private const val PBF = "pbf"
    private const val GZ = "gz"
    private const val PBF_GZ_FORMAT = ".$PBF.$GZ"
    private const val COPILOT = "co-pilot"
    private const val PATH_SEPARATOR = "/"
    private const val HYPHEN = "-"
    private const val UNDERSCORE = "_"

    fun generateFilename(copilotMetadata: CopilotMetadata): String =
        "${copilotMetadata.startedAt}$HISTORY_FILENAME_SEPARATOR${copilotMetadata.endedAt}" +
            "$HISTORY_FILENAME_SEPARATOR$SDK_PLATFORM$HISTORY_FILENAME_SEPARATOR" +
            "${copilotMetadata.navSdkVersion}$HISTORY_FILENAME_SEPARATOR" +
            "${copilotMetadata.navNativeSdkVersion}$HISTORY_FILENAME_SEPARATOR" +
            "$UNDERSCORE$HISTORY_FILENAME_SEPARATOR" +
            "${copilotMetadata.appVersion}$HISTORY_FILENAME_SEPARATOR" +
            "${copilotMetadata.appUserId}$HISTORY_FILENAME_SEPARATOR" +
            "${copilotMetadata.appSessionId}$PBF_GZ_FORMAT"

    fun retrieveOwnerFrom(accessToken: String): String =
        decode(
            accessToken.splitToSequence(".").drop(1).first().replace('-', '+').replace('_', '/'),
        ).getString("u")

    fun generateSessionId(copilotMetadata: CopilotMetadata, owner: String): String =
        "$COPILOT$PATH_SEPARATOR$owner$PATH_SEPARATOR${retrieveSpecVersion()}$PATH_SEPARATOR" +
            "${copilotMetadata.appMode}$PATH_SEPARATOR$HYPHEN$PATH_SEPARATOR$HYPHEN" +
            "$PATH_SEPARATOR${copilotMetadata.driveMode}$PATH_SEPARATOR$HYPHEN" +
            "$PATH_SEPARATOR${copilotMetadata.driveId}"

    fun retrieveSpecVersion(): String = "1.1"

    fun retrieveNavSdkVersion(): String = BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME

    fun retrieveNavNativeSdkVersion(): String =
        BuildConfig.NAV_NATIVE_SDK_VERSION

    fun retrieveIsDebug(): Boolean = BuildConfig.DEBUG

    fun utcTimeNow(format: String, locale: Locale): String {
        val formatter = SimpleDateFormat(format, locale)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    fun delete(file: File): Boolean = file.delete()

    fun size(file: File): Long = file.length()

    suspend fun copyToAndRemove(from: File, filename: String): File =
        withContext(Dispatchers.IO) {
            File(from.parent, filename).also { from.renameTo(it) }
        }

    private fun decode(str: String): JSONObject {
        val requiredLength = (str.length - 1) / 4 * 4 + 4
        return JSONObject(
            String(
                Base64.decode(
                    str.padEnd(requiredLength, '='),
                    Base64.DEFAULT
                )
            )
        )
    }
}
