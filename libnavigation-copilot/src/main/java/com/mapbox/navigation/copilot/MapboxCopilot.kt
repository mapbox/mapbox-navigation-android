package com.mapbox.navigation.copilot

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.copilot.MapboxCopilot.push
import com.mapbox.navigation.copilot.MapboxCopilot.start
import com.mapbox.navigation.copilot.MapboxCopilot.stop
import com.mapbox.navigation.copilot.internal.PushStatusObserver
import com.mapbox.navigation.core.DeveloperMetadataObserver
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.SdkInfoProvider
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import java.util.concurrent.CopyOnWriteArraySet

/**
 * Mapbox Copilot is a Navigation SDK component that collects detailed trace files of navigation sessions together with search analytics data.
 *
 * Copilot collects information that helps you in two different ways:
 * - It makes driver feedback more actionable.
 * - It provides Mapbox teams with the necessary information to investigate and resolve driver-reported issues and, consequently, helps expedite SDK updates that improve the driver experience for the entire developer community.
 *
 * Copilot is a [MapboxNavigationObserver], so it's tied to the [MapboxNavigation] lifecycle automatically.
 * We recommended tracking the [DeveloperMetadata.copilotSessionId] (see [DeveloperMetadataObserver]) so that Mapbox teams can better act on specific end-user feedback. This ID helps Mapbox teams find the respective traces and troubleshoot issues faster.
 *
 * Copilot is an opt-in feature (see [start] and [stop]), which means you have the choice to enable it for your users (drivers).
 * Depending on the use case, you can enable Copilot for either all drivers (for example, during a pilot) or a subset of them.
 * **As the application developer, you are responsible for communicating to drivers about the data that is being collected from their drives, including what kind of data is being collected and when it is collected.**
 *
 * Nav SDK exposes configuration settings (see [NavigationOptions.copilotOptions]) to use Copilot in two ways:
 * 1) Automatic data collection:
 * - Enable Copilot for all trips performed by a specific driver (default option).
 * 2) Manual data collection:
 * - Copilot data is only sent when an end user submits negative feedback about a specific route to help take action on the issue.
 * Data collection for Copilot is tightly coupled to the Navigation SDK Feedback, which means this is only effective if the feedback events are pushed through [MapboxNavigation] Feedback APIs (see [MapboxNavigation.postUserFeedback] and [MapboxNavigation.provideFeedbackMetadataWrapper]).
 *
 * If you would like to provide Search analytics into Copilot, you can send the Search events over to Copilot (see [push]).
 * This information would include whether a routable point for navigation was available.
 * Copilot helps understand the impact of search results to a navigation session (arrival experience, routable points).
 *
 * WARNING: Mapbox Copilot is currently in public-preview. Copilot-related entities and APIs are currently marked as [ExperimentalPreviewMapboxNavigationAPI] and subject to change. These markings will be removed when the feature is generally available.
 *
 * Copilot is a library included in the Navigation SDK that Processes full-trip-trace longitude and latitude data ("**Copilot**"). Copilot is turned off by default and optionally enabled by Customer at the application developer level to improve feedback resolution. If Customer enables Copilot, Customer shall obtain and maintain all necessary consents and permissions, including providing notice to and obtaining End Users' affirmative express consent before any access or use of Copilot.
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

    internal val sdkInformation = SdkInfoProvider.sdkInformation()

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
        if (copilot != null) {
            return
        }
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
