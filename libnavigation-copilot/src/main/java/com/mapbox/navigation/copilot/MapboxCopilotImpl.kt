package com.mapbox.navigation.copilot

import android.content.pm.ApplicationInfo
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.google.gson.Gson
import com.mapbox.common.UploadOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.copyToAndRemove
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.delete
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.generateFilename
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.generateSessionId
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.retrieveNavNativeSdkVersion
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.retrieveNavSdkVersion
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.retrieveOwnerFrom
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.utcTimeNow
import com.mapbox.navigation.copilot.internal.CopilotMetadata
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.directions.session.RoutesObserver
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState.ActiveGuidance
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState.FreeDrive
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState.Idle
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.registerHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.retrieveCopilotHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.unregisterHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.telemetry.UserFeedback
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackCallback
import com.mapbox.navigation.core.internal.telemetry.registerUserFeedbackCallback
import com.mapbox.navigation.core.internal.telemetry.unregisterUserFeedbackCallback
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import java.io.File
import java.util.Locale

/**
 * MapboxCopilot.
 *
 * @property mapboxNavigation
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class MapboxCopilotImpl(
    private val mapboxNavigation: MapboxNavigation,
) {

    private val activeGuidanceHistoryEvents = mutableSetOf<HistoryEventDTO>()
    private val copilotHistoryRecorder = mapboxNavigation.retrieveCopilotHistoryRecorder()
    private var currentHistoryRecordingSessionState: HistoryRecordingSessionState = Idle
        set(value) {
            if (field == value) {
                return
            }
            field = value
            when (field) {
                is ActiveGuidance -> {
                    filterOutActiveGuidance(SEARCH_RESULTS_EVENT_NAME)
                    filterOutActiveGuidance(SEARCH_RESULT_USED_EVENT_NAME)
                    pushOnActiveGuidance()
                }
                else -> {
                    // Do nothing
                }
            }
        }
    private var startSessionTime: Long = 0
    private var appSessionId =
        mapboxNavigation.navigationOptions.eventsAppMetadata?.sessionId ?: "_"
    private var driveId = "_"
    private var startedAt = ""
    private var appUserId = mapboxNavigation.navigationOptions.eventsAppMetadata?.userId ?: "_"
    private var endedAt = ""
    private var driveMode = ""
    private val foregroundBackgroundLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
            push(GoingToForegroundEvent)
        }

        override fun onPause(owner: LifecycleOwner) {
            push(GoingToBackgroundEvent)
        }
    }
    private val accessToken: String = mapboxNavigation.navigationOptions.accessToken ?: ""

    private val shouldSendHistoryOnlyWithFeedback =
        mapboxNavigation.navigationOptions.copilotOptions.shouldSendHistoryOnlyWithFeedback
    private var hasFeedback = false
    private val userFeedbackCallback =
        UserFeedbackCallback { userFeedback ->
            hasFeedback = true
            pushFeedbackEvent(userFeedback)
        }
    private val historyRecordingStateChangeObserver = object : HistoryRecordingStateChangeObserver {
        override fun onShouldStartRecording(state: HistoryRecordingSessionState) {
            if (state !is Idle) {
                startRecordingHistory(state)
            }
        }

        override fun onShouldStopRecording(state: HistoryRecordingSessionState) {
            uploadHistory()
        }

        override fun onShouldCancelRecording(state: HistoryRecordingSessionState) {
            stopRecording { historyFilePath ->
                delete(File(historyFilePath))
            }
        }
    }
    private var initRoute = false
    private lateinit var routesObserver: RoutesObserver

    /**
     * start
     */
    fun start() {
        registerUserFeedbackCallback(userFeedbackCallback)
        MapboxNavigationApp.lifecycleOwner.lifecycle.addObserver(
            foregroundBackgroundLifecycleObserver
        )
        mapboxNavigation.registerHistoryRecordingStateChangeObserver(
            historyRecordingStateChangeObserver
        )
    }

    /**
     * stop
     */
    fun stop() {
        unregisterUserFeedbackCallback(userFeedbackCallback)
        MapboxNavigationApp.lifecycleOwner.lifecycle.removeObserver(
            foregroundBackgroundLifecycleObserver
        )
        mapboxNavigation.unregisterHistoryRecordingStateChangeObserver(
            historyRecordingStateChangeObserver
        )
        uploadHistory()
    }

    /**
     * push
     */
    fun push(historyEvent: HistoryEvent) {
        val eventType = historyEvent.snakeCaseEventName
        val eventJson = toEventJson(historyEvent.eventDTO)
        when (historyEvent) {
            is InitRouteEvent -> {
                addActiveGuidance(eventType, eventJson)
                pushOnActiveGuidance()
            }
            is DriveEndsEvent, GoingToBackgroundEvent, GoingToForegroundEvent -> {
                pushHistoryJson(eventType, eventJson)
            }
            is NavFeedbackSubmittedEvent -> {
                pushOnFreeDriveOrActiveGuidance(eventType, eventJson)
            }
            is SearchResultsEvent -> {
                addActiveGuidance(eventType, eventJson)
                pushSearchResults(eventType, eventJson)
            }
            is SearchResultUsedEvent -> {
                addActiveGuidance(eventType, eventJson)
                pushOnActiveGuidance()
            }
        }
    }

    private fun filterOutActiveGuidance(type: String) {
        val filtered = activeGuidanceHistoryEvents.filter { it.type == type }
        if (filtered.size > 1) {
            for (count in 0 until filtered.size - 1) {
                activeGuidanceHistoryEvents.remove(filtered[count])
            }
        }
    }

    private fun pushOnActiveGuidance() {
        if (currentHistoryRecordingSessionState is ActiveGuidance) {
            activeGuidanceHistoryEvents.forEach {
                pushHistoryJson(it.type, it.json)
            }
            activeGuidanceHistoryEvents.clear()
        }
    }

    private fun pushHistoryJson(eventType: String, eventJson: String) {
        copilotHistoryRecorder.pushHistory(eventType, eventJson)
    }

    private fun pushFeedbackEvent(userFeedback: UserFeedback) {
        val lat = userFeedback.location.latitude()
        val lng = userFeedback.location.longitude()
        val feedbackId = userFeedback.feedbackId
        val feedbackType = userFeedback.feedbackType
        val feedbackSubType = userFeedback.feedbackSubType?.toHashSet().orEmpty()
        val feedbackEvent = NavFeedbackSubmitted(
            feedbackId,
            feedbackType,
            feedbackSubType,
            HistoryPoint(lat, lng),
        )
        push(NavFeedbackSubmittedEvent(feedbackEvent))
    }

    private fun startRecordingHistory(
        historyRecordingSessionState: HistoryRecordingSessionState
    ) {
        copilotHistoryRecorder.startRecording()
        currentHistoryRecordingSessionState = historyRecordingSessionState
        startSessionTime = System.currentTimeMillis()
        startedAt = currentUtcTime()
        driveId = historyRecordingSessionState.sessionId
        driveMode = when (historyRecordingSessionState) {
            is ActiveGuidance -> "active-guidance"
            is FreeDrive -> "free-drive"
            else -> throw IllegalArgumentException("Should not try and track idle state")
        }
        routesObserver = RoutesObserver { routesResult ->
            val navigationRoutes = routesResult.navigationRoutes
            if (initialRoute(navigationRoutes)) {
                push(
                    InitRouteEvent(
                        InitRoute(
                            routesResult.navigationRoutes.first().directionsRoute.requestUuid(),
                            routesResult.navigationRoutes.first().directionsRoute,
                        )
                    ),
                )
                initRoute = true
            }
        }.also {
            mapboxNavigation.registerRoutesObserver(it)
        }
    }

    private fun currentUtcTime(
        format: String = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
        locale: Locale = Locale.US,
    ): String {
        return utcTimeNow(format, locale)
    }

    private fun initialRoute(navigationRoutes: List<NavigationRoute>): Boolean =
        currentHistoryRecordingSessionState is ActiveGuidance &&
            navigationRoutes.isNotEmpty() && !initRoute

    private fun toEventJson(event: EventDTO): String {
        val eventJson = gson.toJson(event) ?: ""
        check(eventJson != "null") {
            "The event did not convert to json: $eventJson $event"
        }
        return eventJson
    }

    private fun uploadHistory() {
        if (currentHistoryRecordingSessionState is Idle) {
            return
        }
        endedAt = currentUtcTime()
        val diffTime = System.currentTimeMillis() - startSessionTime
        push(DriveEndsEvent(DriveEnds(DriveEndsType.CanceledManually.type, diffTime)))
        val drive = buildNavigationSession()
        stopRecording { historyFilePath ->
            if (hasFeedback || !shouldSendHistoryOnlyWithFeedback) {
                pushHistoryFile(historyFilePath, drive)
            }
        }
    }

    private fun buildNavigationSession(): CopilotMetadata {
        val context = mapboxNavigation.navigationOptions.applicationContext
        return CopilotMetadata(
            if (isAppDebuggable()) {
                "mbx-debug"
            } else {
                "mbx-prod"
            },
            driveMode,
            driveId,
            startedAt,
            endedAt,
            retrieveNavSdkVersion(),
            retrieveNavNativeSdkVersion(),
            context.packageManager.getPackageInfo(
                mapboxNavigation.navigationOptions.applicationContext.packageName,
                0,
            ).versionName,
            appUserId,
            appSessionId,
        )
    }

    private fun isAppDebuggable(): Boolean {
        val context = mapboxNavigation.navigationOptions.applicationContext
        return context.applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE != 0
    }

    private fun pushHistoryFile(historyFilePath: String, drive: CopilotMetadata) {
        val metadata = buildAttachmentMetadata(drive)
        val uploadOptions = buildUploadOptions(historyFilePath, metadata)
        HistoryUploadWorker.uploadHistory(
            mapboxNavigation.navigationOptions.applicationContext,
            drive,
            uploadOptions,
            metadata.sessionId,
        )
    }

    private fun buildAttachmentMetadata(copilotMetadata: CopilotMetadata): AttachmentMetadata {
        val filename = generateFilename(copilotMetadata)
        val owner = retrieveOwnerFrom(accessToken)
        val sessionId = generateSessionId(copilotMetadata, owner)
        return AttachmentMetadata(
            name = filename,
            created = startedAt,
            fileId = "",
            format = GZ,
            type = ZIP,
            sessionId = sessionId,
        )
    }

    private fun buildUploadOptions(
        filePath: String,
        metadata: AttachmentMetadata,
    ): UploadOptions {
        val metadataList = arrayListOf(metadata)

        // we have to leave it up to end users to ensure the filename in the metadata matches the actual name of the file
        val from = File(filePath)
        val to = copyToAndRemove(from, metadata.name)
        var url = if (HistoryAttachmentsUtils.retrieveIsDebug()) {
            STAGING_BASE_URL
        } else {
            PROD_BASE_URL
        }
        url += "/attachments/v1?access_token=$accessToken"

        return UploadOptions(
            to.absolutePath,
            url,
            HashMap(),
            gson.toJson(metadataList),
            MEDIA_TYPE_ZIP,
        )
    }

    private fun stopRecording(callback: (String) -> Unit) {
        copilotHistoryRecorder.stopRecording { historyFilePath ->
            historyFilePath ?: return@stopRecording
            callback(historyFilePath)
        }
        mapboxNavigation.unregisterRoutesObserver(routesObserver)
        initRoute = false
        hasFeedback = false
        if (currentHistoryRecordingSessionState is ActiveGuidance) {
            activeGuidanceHistoryEvents.clear()
        }
        currentHistoryRecordingSessionState = Idle
    }

    private fun addActiveGuidance(eventType: String, eventJson: String) {
        activeGuidanceHistoryEvents.add(HistoryEventDTO(eventType, eventJson))
    }

    private fun pushOnFreeDriveOrActiveGuidance(eventType: String, eventJson: String) {
        if (currentHistoryRecordingSessionState !is Idle) {
            pushHistoryJson(eventType, eventJson)
        }
    }

    private fun pushSearchResults(eventType: String, eventJson: String) {
        when (currentHistoryRecordingSessionState) {
            is ActiveGuidance -> {
                pushOnActiveGuidance()
            }
            is FreeDrive -> {
                pushHistoryJson(eventType, eventJson)
            }
            is Idle -> {
                // Do nothing as we're not recording
            }
        }
    }

    internal companion object {

        internal val gson = Gson()
        internal const val GZ = "gz"
        internal const val ZIP = "zip"
        internal const val MEDIA_TYPE_ZIP = "application/zip"
        internal const val LOG_CATEGORY = "MapboxCopilot"
        private const val STAGING_BASE_URL = "https://api-events-staging.tilestream.net"
        private const val PROD_BASE_URL = "https://events.mapbox.com"
    }
}
