package com.mapbox.navigation.copilot.internal

import com.mapbox.navigation.base.options.NavigationOptions
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.retrieveNavNativeSdkVersion
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.retrieveNavSdkVersion

/**
 * Copilot metadata.
 *
 * @property appMode mode of the application / environment
 * @property driveMode either active-guidance or free-drive
 * @property driveId unique identifier of a single drive (_Active Guidance_ or _Free Drive_)
 * @property startedAt UTC timestamp in yyyy-mm-ddThh:MM:ss.msZ format
 * @property endedAt UTC timestamp in yyyy-mm-ddThh:MM:ss.msZ format
 * @property navSdkVersion Navigation SDK version used
 * @property navNativeSdkVersion NN version used
 * @property appVersion of the application
 * @property appUserId [EventsAppMetadata] user identifier (optional). See [NavigationOptions] from [MapboxNavigation]
 * @property appSessionId [EventsAppMetadata] session identifier (optional). See [NavigationOptions] from [MapboxNavigation]
 */
data class CopilotMetadata(
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
)
