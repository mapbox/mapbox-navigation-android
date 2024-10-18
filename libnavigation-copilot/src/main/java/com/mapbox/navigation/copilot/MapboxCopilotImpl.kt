package com.mapbox.navigation.copilot

import android.app.Application
import android.content.Context
import android.os.SystemClock
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import com.google.gson.GsonBuilder
import com.mapbox.api.directions.v5.DirectionsAdapterFactory
import com.mapbox.geojson.Point
import com.mapbox.geojson.PointAsCoordinatesTypeAdapter
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.options.DeviceType
import com.mapbox.navigation.base.trip.model.RouteLegProgress
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.copilot.HistoryAttachmentsUtils.delete
import com.mapbox.navigation.copilot.internal.CopilotSession
import com.mapbox.navigation.copilot.internal.currentUtcTime
import com.mapbox.navigation.copilot.internal.saveFilename
import com.mapbox.navigation.copilot.work.HistoryUploadWorker
import com.mapbox.navigation.copilot.work.PeriodicHistoryCleanupWorker
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.arrival.ArrivalObserver
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState.ActiveGuidance
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState.FreeDrive
import com.mapbox.navigation.core.internal.HistoryRecordingSessionState.Idle
import com.mapbox.navigation.core.internal.HistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.registerHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.extensions.retrieveCopilotHistoryRecorder
import com.mapbox.navigation.core.internal.extensions.unregisterHistoryRecordingStateChangeObserver
import com.mapbox.navigation.core.internal.history.HistoryFiles
import com.mapbox.navigation.core.internal.lifecycle.CarAppLifecycleOwner
import com.mapbox.navigation.core.internal.telemetry.ExtendedUserFeedback
import com.mapbox.navigation.core.internal.telemetry.UserFeedbackObserver
import com.mapbox.navigation.core.internal.telemetry.registerUserFeedbackObserver
import com.mapbox.navigation.core.internal.telemetry.unregisterUserFeedbackObserver
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.utils.internal.InternalJobControlFactory
import com.mapbox.navigation.utils.internal.logD
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

/**
 * MapboxCopilot.
 *
 * @property mapboxNavigation
 */
@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal class MapboxCopilotImpl(
    private val mapboxNavigation: MapboxNavigation,
) : HistoryRecordingStateChangeObserver {

    private val applicationContext: Context
        get() = mapboxNavigation.navigationOptions.applicationContext
    private val mainJobController by lazy { InternalJobControlFactory.createMainScopeJobControl() }
    private val activeGuidanceHistoryEvents = mutableSetOf<HistoryEventDTO>()
    private val copilotHistoryRecorder = mapboxNavigation.retrieveCopilotHistoryRecorder()
    private var currentHistoryRecordingSessionState: HistoryRecordingSessionState = Idle
        set(value) {
            if (field == value) {
                return
            }
            field = value
            if (field is ActiveGuidance) {
                filterOutActiveGuidance(SEARCH_RESULTS_EVENT_NAME)
                filterOutActiveGuidance(SEARCH_RESULT_USED_EVENT_NAME)
            }
        }
    private var startSessionTime: Long = 0
    private var activeSession: CopilotSession = CopilotSession()
    private val finishedSessions = mutableListOf<CopilotSession>()
    private val filepaths = HistoryFiles(applicationContext)

    private val appLifecycleObserver = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) = push(GoingToForegroundEvent)
        override fun onPause(owner: LifecycleOwner) = push(GoingToBackgroundEvent)
    }
    private val deviceType = mapboxNavigation.navigationOptions.deviceProfile.deviceType
    private val copilotOptions
        get() = mapboxNavigation.navigationOptions.copilotOptions
    private val shouldSendHistoryOnlyWithFeedback
        get() = copilotOptions.shouldSendHistoryOnlyWithFeedback
    private val maxHistoryFileLengthMilliseconds
        get() = copilotOptions.maxHistoryFileLengthMillis
    private val maxHistoryFilesPerSession
        get() = copilotOptions.maxHistoryFilesPerSession
    private val maxTotalHistoryFilesSizePerSession
        get() = copilotOptions.maxTotalHistoryFilesSizePerSession
    private val shouldRecordFreeDriveHistories
        get() = copilotOptions.shouldRecordFreeDriveHistories

    private var hasFeedback = false
    private val userFeedbackObserver =
        UserFeedbackObserver { userFeedback ->
            hasFeedback = true
            pushFeedbackEvent(userFeedback)
        }

    private var arrivedAtFinalDestination = false
    private val arrivalObserver = object : ArrivalObserver {
        override fun onWaypointArrival(routeProgress: RouteProgress) = Unit
        override fun onNextRouteLegStart(routeLegProgress: RouteLegProgress) = Unit
        override fun onFinalDestinationArrival(routeProgress: RouteProgress) {
            arrivedAtFinalDestination = true
        }
    }
    private val appLifecycleOwnerCleanupAction: () -> Unit
    private val appLifecycleOwner = if (MapboxNavigationApp.isSetup()) {
        appLifecycleOwnerCleanupAction = { }
        MapboxNavigationApp.lifecycleOwner
    } else {
        CarAppLifecycleOwner().apply {
            val application = applicationContext as Application
            attachAllActivities(application)
            appLifecycleOwnerCleanupAction = {
                detachAllActivities(application)
            }
        }
    }

    private var restartRecordingHistoryJob: Job? = null

    override fun onShouldStartRecording(state: HistoryRecordingSessionState) {
        if (isRecordingAllowed(state)) {
            startRecordingHistory(state)
        }
    }

    override fun onShouldStopRecording(state: HistoryRecordingSessionState) {
        uploadRecording()
    }

    override fun onShouldCancelRecording(state: HistoryRecordingSessionState) {
        cancelRecordingHistory()
    }

    /**
     * start
     */
    fun start() {
        mapboxNavigation.registerUserFeedbackObserver(userFeedbackObserver)
        appLifecycleOwner.lifecycle.addObserver(appLifecycleObserver)
        mapboxNavigation.registerHistoryRecordingStateChangeObserver(this)
        PeriodicHistoryCleanupWorker.scheduleWork(
            applicationContext,
            filepaths.copilotAbsolutePath(),
        )
    }

    /**
     * stop
     */
    fun stop() {
        mapboxNavigation.unregisterUserFeedbackObserver(userFeedbackObserver)
        appLifecycleOwner.lifecycle.removeObserver(appLifecycleObserver)
        appLifecycleOwnerCleanupAction()
        mapboxNavigation.unregisterHistoryRecordingStateChangeObserver(this)
        uploadRecording()
    }

    /**
     * push
     */
    fun push(historyEvent: HistoryEvent) {
        val eventType = historyEvent.snakeCaseEventName
        val eventJson = toEventJson(historyEvent.eventDTO)
        when (historyEvent) {
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
        logD("pushHistory event=$eventType")
        copilotHistoryRecorder.pushHistory(eventType, eventJson)
    }

    private fun pushFeedbackEvent(userFeedback: ExtendedUserFeedback) {
        val lat = userFeedback.location.latitude()
        val lng = userFeedback.location.longitude()
        val feedbackId = userFeedback.feedbackId
        val feedbackType = userFeedback.feedback.feedbackType
        val feedbackSubType = userFeedback.feedback.feedbackSubTypes?.toHashSet().orEmpty()
        val feedbackEvent = NavFeedbackSubmitted(
            feedbackId,
            feedbackType,
            feedbackSubType,
            HistoryPoint(lat, lng),
        )
        push(NavFeedbackSubmittedEvent(feedbackEvent))
    }

    private fun startRecordingHistory(
        historyRecordingSessionState: HistoryRecordingSessionState,
    ) {
        val driveMode = when (historyRecordingSessionState) {
            is ActiveGuidance -> "active-guidance"
            is FreeDrive -> "free-drive"
            else -> throw IllegalArgumentException("Should not try and track idle state")
        }

        val recording = copilotHistoryRecorder.startRecording().firstOrNull() ?: ""
        currentHistoryRecordingSessionState = historyRecordingSessionState
        startSessionTime = SystemClock.elapsedRealtime()
        activeSession = CopilotSession.create(
            navigationOptions = mapboxNavigation.navigationOptions,
            driveId = historyRecordingSessionState.sessionId,
            driveMode = driveMode,
            recording = recording,
        )
        logD("startRecording $activeSession")
        saveCopilotSession()

        mapboxNavigation.registerArrivalObserver(arrivalObserver)
        restartRecordingHistoryJob = mainJobController.scope.launch {
            while (true) {
                delay(maxHistoryFileLengthMilliseconds)
                restartRecordingHistory()
                saveCopilotSession()
            }
        }
    }

    private fun toEventJson(event: EventDTO): String {
        val eventJson = gson.toJson(event) ?: ""
        check(eventJson != "null") {
            "The event did not convert to json: $eventJson $event"
        }
        return eventJson
    }

    private fun restartRecordingHistory() {
        val session = activeSession.copy(endedAt = currentUtcTime())
        logD("stopRecording $session")
        copilotHistoryRecorder.stopRecording { historyFilePath ->
            historyFilePath ?: return@stopRecording
            finishedSessions.add(session)
            if (finishedSessions.size == maxHistoryFilesPerSession) {
                val oldestSession = finishedSessions.removeAt(0)
                delete(File(oldestSession.recording))
            }
            limitTotalHistoryFilesSize(finishedSessions)
        }
        val recording = copilotHistoryRecorder.startRecording().firstOrNull() ?: ""
        // when restarting recording we inherit previous session info and just update start time
        activeSession = activeSession.copy(
            recording = recording,
            startedAt = currentUtcTime(),
        )
        logD("startRecording $activeSession")
    }

    private fun uploadRecording() {
        if (!isRecordingAllowed(currentHistoryRecordingSessionState)) {
            return
        }
        if (hasFeedback || !shouldSendHistoryOnlyWithFeedback) {
            val session = activeSession.copy(endedAt = currentUtcTime())

            pushOnActiveGuidance()
            pushDriveEndsEvent()

            val historyFilesCopy = finishedSessions.toMutableList()
            logD("stopRecording $session")
            stopRecording {
                historyFilesCopy.add(session)
                limitTotalHistoryFilesSize(historyFilesCopy)
                pushHistoryFiles(historyFilesCopy)
            }
        } else {
            cancelRecordingHistory()
        }
    }

    private fun cancelRecordingHistory() {
        finishedSessions.forEach { session ->
            delete(File(session.recording))
        }
        stopRecording { historyFilePath ->
            delete(File(historyFilePath))
            deleteCopilotSession()
            activeSession = CopilotSession()
        }
    }

    private fun limitTotalHistoryFilesSize(historyFiles: MutableList<CopilotSession>) {
        var total = historyFiles.sumOf { HistoryAttachmentsUtils.size(File(it.recording)) }
        while (total > maxTotalHistoryFilesSizePerSession) {
            val oldestSession = historyFiles.removeAt(0)
            val firstFile = File(oldestSession.recording)
            total -= HistoryAttachmentsUtils.size(firstFile)
            delete(firstFile)
        }
    }

    private fun pushHistoryFiles(copilotSessions: List<CopilotSession>) {
        copilotSessions.forEach { session ->
            HistoryUploadWorker.uploadHistory(
                context = applicationContext,
                copilotSession = session,
            )
        }
    }

    private fun saveCopilotSession() = mainJobController.scope.launch(Dispatchers.IO) {
        val file = File(filepaths.copilotAbsolutePath(), activeSession.saveFilename())
        file.writeText(activeSession.toJson())
    }

    private fun deleteCopilotSession() = mainJobController.scope.launch(Dispatchers.IO) {
        delete(File(filepaths.copilotAbsolutePath(), activeSession.saveFilename()))
    }

    private fun stopRecording(callback: (String) -> Unit) {
        restartRecordingHistoryJob?.cancel()
        restartRecordingHistoryJob = null
        copilotHistoryRecorder.stopRecording { historyFilePath ->
            historyFilePath ?: return@stopRecording
            callback(historyFilePath)
        }
        hasFeedback = false
        mapboxNavigation.unregisterArrivalObserver(arrivalObserver)
        arrivedAtFinalDestination = false
        if (currentHistoryRecordingSessionState is ActiveGuidance) {
            activeGuidanceHistoryEvents.clear()
        }
        currentHistoryRecordingSessionState = Idle
        finishedSessions.clear()
    }

    private fun addActiveGuidance(eventType: String, eventJson: String) {
        activeGuidanceHistoryEvents.add(HistoryEventDTO(eventType, eventJson))
    }

    private fun pushOnFreeDriveOrActiveGuidance(eventType: String, eventJson: String) {
        if (isRecordingAllowed(currentHistoryRecordingSessionState)) {
            pushHistoryJson(eventType, eventJson)
        }
    }

    private fun pushDriveEndsEvent() {
        val diffTime = SystemClock.elapsedRealtime() - startSessionTime
        val driveEndsType = when {
            arrivedAtFinalDestination -> {
                when (deviceType) {
                    DeviceType.HANDHELD -> DriveEndsType.Arrived
                    DeviceType.AUTOMOBILE -> DriveEndsType.VehicleParked
                }
            }

            appLifecycleOwner.lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED) -> {
                DriveEndsType.CanceledManually
            }

            else -> {
                DriveEndsType.ApplicationClosed
            }
        }
        push(DriveEndsEvent(DriveEnds(driveEndsType.type, diffTime)))
    }

    private fun pushSearchResults(eventType: String, eventJson: String) {
        when (currentHistoryRecordingSessionState) {
            is ActiveGuidance -> {
                // Will be pushed at the end of the session
            }

            is FreeDrive -> {
                if (shouldRecordFreeDriveHistories) {
                    pushHistoryJson(eventType, eventJson)
                }
            }

            is Idle -> {
                // Do nothing as we're not recording
            }
        }
    }

    private fun isRecordingAllowed(state: HistoryRecordingSessionState): Boolean {
        return state is ActiveGuidance || state is FreeDrive && shouldRecordFreeDriveHistories
    }

    internal companion object {

        internal val gson = GsonBuilder()
            .registerTypeAdapterFactory(DirectionsAdapterFactory.create())
            .registerTypeAdapter(Point::class.java, PointAsCoordinatesTypeAdapter())
            .create()
        internal const val GZ = "gz"
        internal const val ZIP = "zip"
        internal const val MEDIA_TYPE_ZIP = "application/zip"
        internal const val LOG_CATEGORY = "MapboxCopilot"
        internal const val PROD_BASE_URL = "https://events.mapbox.com"

        internal fun logD(msg: String) = logD(msg, LOG_CATEGORY)
    }
}
