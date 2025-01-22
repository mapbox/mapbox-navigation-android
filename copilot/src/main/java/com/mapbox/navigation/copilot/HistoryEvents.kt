package com.mapbox.navigation.copilot

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

internal interface EventDTO

/**
 * Custom history event hierarchy definition.
 *
 * Can't be extended externally as set of custom events are defined and managed internally.
 */
@ExperimentalPreviewMapboxNavigationAPI
sealed class HistoryEvent(internal val snakeCaseEventName: String, internal val eventDTO: EventDTO)

internal const val SEARCH_RESULTS_EVENT_NAME = "search_results"
internal const val SEARCH_RESULT_USED_EVENT_NAME = "search_result_used"
internal const val DRIVE_ENDS_EVENT_NAME = "drive_ends"
internal const val GOING_TO_FOREGROUND_EVENT_NAME = "going_to_foreground"
internal const val GOING_TO_BACKGROUND_EVENT_NAME = "going_to_background"
internal const val NAV_FEEDBACK_SUBMITTED_EVENT_NAME = "nav_feedback_submitted"

/**
 * SearchResultsEvent.
 *
 * It should be pushed every time a search request response is retrieved.
 *
 * @property searchResults [SearchResults] from search request
 */
@ExperimentalPreviewMapboxNavigationAPI
class SearchResultsEvent(val searchResults: SearchResults) : HistoryEvent(
    SEARCH_RESULTS_EVENT_NAME,
    searchResults,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchResultsEvent

        if (snakeCaseEventName != other.snakeCaseEventName) return false
        return searchResults == other.searchResults
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = snakeCaseEventName.hashCode()
        result = 31 * result + searchResults.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SearchResultsEvent(" +
            "snakeCaseEventName=$snakeCaseEventName, " +
            "searchResults=$searchResults" +
            ")"
    }
}

/**
 * SearchResultUsedEvent.
 *
 * It should be pushed every time a search result is selected.
 *
 * @property searchResultUsed
 */
@ExperimentalPreviewMapboxNavigationAPI
class SearchResultUsedEvent(val searchResultUsed: SearchResultUsed) : HistoryEvent(
    SEARCH_RESULT_USED_EVENT_NAME,
    searchResultUsed,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as SearchResultUsedEvent

        if (snakeCaseEventName != other.snakeCaseEventName) return false
        return searchResultUsed == other.searchResultUsed
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        var result = snakeCaseEventName.hashCode()
        result = 31 * result + searchResultUsed.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "SearchResultUsedEvent(" +
            "snakeCaseEventName=$snakeCaseEventName, " +
            "searchResultUsed=$searchResultUsed" +
            ")"
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal data class DriveEndsEvent(val driveEnds: DriveEnds) :
    HistoryEvent(DRIVE_ENDS_EVENT_NAME, driveEnds)

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal object GoingToForegroundEvent :
    HistoryEvent(GOING_TO_FOREGROUND_EVENT_NAME, GoingToForeground)

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal object GoingToBackgroundEvent :
    HistoryEvent(GOING_TO_BACKGROUND_EVENT_NAME, GoingToBackground)

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
internal data class NavFeedbackSubmittedEvent(val navFeedbackSubmitted: NavFeedbackSubmitted) :
    HistoryEvent(NAV_FEEDBACK_SUBMITTED_EVENT_NAME, navFeedbackSubmitted)
