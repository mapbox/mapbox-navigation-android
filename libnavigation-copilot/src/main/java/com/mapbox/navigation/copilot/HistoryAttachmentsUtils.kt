package com.mapbox.navigation.copilot

import android.util.Base64
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.core.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@Suppress("MaxLineLength")
internal object HistoryAttachmentsUtils {

    private const val PBF_GZ_FORMAT = "pbf.gz"
    private const val COPILOT = "co-pilot"

    /* ktlint-disable max-line-length */
    // Eg. output
    //   2022-05-12T17:47:42.353Z__2022-05-12T17:48:12.504Z__android__2.7.0-beta.1__108.0.1_____v0.108.0-9-g0527ee4__wBzYwfK0oCYMTNYPIFHhYuYOLLs1__3e48fd7b-ac82-42a8-9abe-aaeb724f92ce.pbf.gz
    fun attachmentFilename(copilotSession: CopilotSession, extension: String = PBF_GZ_FORMAT): String = with(
        copilotSession,
    ) {
        "${startedAt}__${endedAt}__android__${navSdkVersion}__${navNativeSdkVersion}_____${appVersion}__${appUserId}__$appSessionId.$extension"
    }
    /* ktlint-enable max-line-length */

    fun retrieveOwnerFrom(accessToken: String): String =
        decode(
            accessToken.splitToSequence(".")
                .drop(1)
                .first()
                .replace('-', '+')
                .replace('_', '/'),
        ).getString("u")

    fun generateSessionId(copilotSession: CopilotSession, owner: String): String =
        "$COPILOT/$owner/${retrieveSpecVersion()}/" +
            "${copilotSession.appMode}/-/-" +
            "/${copilotSession.driveMode}/-" +
            "/${copilotSession.driveId}"

    fun retrieveSpecVersion(): String = "1.2"

    fun retrieveNavSdkVersion(): String = BuildConfig.MAPBOX_NAVIGATION_VERSION_NAME

    fun retrieveNavNativeSdkVersion(): String =
        BuildConfig.NAV_NATIVE_SDK_VERSION

    fun utcTimeNow(format: String, locale: Locale): String {
        val formatter = SimpleDateFormat(format, locale)
        formatter.timeZone = TimeZone.getTimeZone("UTC")
        return formatter.format(Date())
    }

    fun delete(file: File): Boolean = file.delete()

    fun size(file: File): Long = file.length()

    suspend fun rename(from: File, filename: String): File =
        withContext(Dispatchers.IO) {
            File(from.parent, filename).also { from.renameTo(it) }
        }

    private fun decode(str: String): JSONObject {
        val requiredLength = (str.length - 1) / 4 * 4 + 4
        return JSONObject(
            String(
                Base64.decode(
                    str.padEnd(requiredLength, '='),
                    Base64.DEFAULT,
                ),
            ),
        )
    }
}
