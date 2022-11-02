package com.mapbox.navigation.core.history

/**
 * Drive
 *
 * @property sessionId
 * @property startedAt
 * @property userId
 * @property endedAt
 * @property historyStoragePath
 * @property driveMode
 * @property appVersion
 * @property appMode
 * @property navSdkVersion
 * @property navNativeSdkVersion
 * @property appSessionId
 */
data class Drive(
    val sessionId: String,
    val startedAt: String,
    val userId: String,
    val endedAt: String,
    val historyStoragePath: String,
    val driveMode: String,
    val appVersion: String,
    val appMode: String,
    val navSdkVersion: String,
    val navNativeSdkVersion: String,
    val appSessionId: String?,
)
