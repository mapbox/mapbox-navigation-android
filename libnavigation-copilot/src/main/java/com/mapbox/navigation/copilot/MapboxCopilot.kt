package com.mapbox.navigation.copilot

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.copilot.internal.PushStatusObserver
import com.mapbox.navigation.core.DeveloperMetadataObserver
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Mapbox Copilot is a Navigation SDK component that collects detailed trace files of navigation sessions together with search analytics data.
 *
 * Mapbox Copilot collects information that helps customers in two different ways:
 * - It makes the feedback provided by drivers more actionable.
 * - It provides Mapbox teams with the necessary information to investigate and resolve driver-reported issues and, consequently, helps expedite SDK updates that improve the driver experience for the entire developer community.
 *
 * It's a [MapboxNavigationObserver] so it's tied to [MapboxNavigation] lifecycle automatically.
 * It's highly recommended to keep track of the [DeveloperMetadata.copilotSessionId] (see [DeveloperMetadataObserver]) so that Mapbox teams can easily act on specific end-users feedback as Mapbox teams can easily find the respective traces and troubleshoot issues faster.
 *
 * Mapbox Copilot is an opt-in by the customer (see [start] and [stop]) i.e. Navigation SDK customers have the choice to enable it for their drivers.
 * Depending on the use case, some customers can enable Copilot to all drivers (e.g. during a pilot) or just a subset of them.
 * **The application developer is responsible for communicating to their drivers when and what data is being collected from their drives.**
 *
 * Nav SDK exposes configuration settings (see [NavigationOptions.copilotOptions]) to use Copilot in two ways:
 * 1) Automatic data collection:
 * - Copilot will be enabled for all trips performed by a specific driver (default option).
 * 2) Manual data collection:
 * - Copilot data will only be sent when an end user submits negative feedback about a specific route to help take action on the issue.
 * Noting that this is tightly coupled to the Navigation SDK Feedback i.e. this is only effective if the feedback events are pushed through [MapboxNavigation] Feedback APIs (see [MapboxNavigation.postUserFeedback] and [MapboxNavigation.provideFeedbackMetadataWrapper]).
 *
 * If you would like to provide Search analytics into Copilot, you can send the Search events over to Copilot (see [push]).
 * This information would include whether a routable point for navigation was available.
 * Copilot helps understand the impact of search results to a navigation session (arrival experience, routable points).
 *
 * WARNING: Mapbox Copilot is currently in public-preview and hence Copilot related entities and APIs are currently marked as [ExperimentalPreviewMapboxNavigationAPI] and subject to change. These will be removed when going GA.
 *
 * @see [NavigationOptions.copilotOptions]
 * @see [MapboxNavigation.navigationOptions] [EventsAppMetadata]
 * @see [MapboxNavigation.registerDeveloperMetadataObserver] [MapboxNavigation.unregisterDeveloperMetadataObserver] and [DeveloperMetadataObserver]
 * @see [MapboxNavigation.postUserFeedback] and [MapboxNavigation.provideFeedbackMetadataWrapper]
 */
@ExperimentalPreviewMapboxNavigationAPI
object MapboxCopilot : MapboxNavigationObserver {

    private var copilot: MapboxCopilotImpl? = null
    internal val pushStatusObservers = CopyOnWriteArraySet<PushStatusObserver>()

    /**
     * Starts Copilot.
     */
    fun start() {
        MapboxNavigationApp.registerObserver(this)
    }

    /**
     * Stops Copilot.
     */
    fun stop() {
        MapboxNavigationApp.unregisterObserver(this)
    }

    /**
     * Pushes search events into Copilot.
     *
     * @param historyEvent [SearchResultsEvent] or [SearchResultUsedEvent]
     */
    fun push(historyEvent: HistoryEvent) {
        copilot?.push(historyEvent)
    }

    /**
     * Signals that the [mapboxNavigation] instance is ready for use. Use this function to
     * register [mapboxNavigation] observers, such as [MapboxNavigation.registerRoutesObserver].
     *
     * @param mapboxNavigation instance that is being attached
     */
    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        copilot = MapboxCopilotImpl(mapboxNavigation).also { it.start() }
    }

    /**
     * Signals that the [mapboxNavigation] instance is being detached. Use this function to
     * unregister [mapboxNavigation] observers that were registered in [onAttached].
     *
     * @param mapboxNavigation instance that is being detached
     */
    override fun onDetached(mapboxNavigation: MapboxNavigation) {
        copilot?.stop()
        copilot = null
    }
}
