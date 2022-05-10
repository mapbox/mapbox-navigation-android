package com.mapbox.androidauto.car.feedback.ui

import androidx.annotation.Keep
import androidx.car.app.CarContext
import com.mapbox.androidauto.R
import com.mapbox.androidauto.car.feedback.core.CarFeedbackItemProvider
import com.mapbox.api.geocoding.v5.models.GeocodingResponse
import com.mapbox.navigation.core.geodeeplink.GeoDeeplink
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.search.analytics.FeedbackEvent.FeedbackReason.Companion.INCORRECT_ADDRESS
import com.mapbox.search.analytics.FeedbackEvent.FeedbackReason.Companion.INCORRECT_LOCATION
import com.mapbox.search.analytics.FeedbackEvent.FeedbackReason.Companion.INCORRECT_NAME
import com.mapbox.search.analytics.FeedbackEvent.FeedbackReason.Companion.OTHER
import com.mapbox.search.record.FavoriteRecord
import com.mapbox.search.result.SearchSuggestion

/**
 * This object is converted to json and sent the navigation history as a custom event.
 *
 * TODO add builder
 */
@SuppressWarnings("LongParameterList")
@Keep
class CarFeedbackItem(
    val carFeedbackTitle: String,
    val carFeedbackIcon: CarFeedbackIcon,
    @FeedbackEvent.Type
    val navigationFeedbackType: String? = null,
    @com.mapbox.search.analytics.FeedbackEvent.FeedbackReason
    val searchFeedbackReason: String? = null,
    val favoritesFeedbackReason: String? = null,
    val geoDeeplink: GeoDeeplink? = null,
    val geocodingResponse: GeocodingResponse? = null,
    val favoriteRecords: List<FavoriteRecord>? = null,
    val searchSuggestions: List<SearchSuggestion>? = null,
)

/**
 * TODO These can all be moved to their features. Currently implementing in one file while
 *   defining the object definitions.
 */

// Pulled from FeedbackHelper.getFreeDriveFeedbackTypes()
fun buildFreeDriveFeedbackItems(carContext: CarContext) = listOf(
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_incorrect_visual),
        carFeedbackIcon = IncorrectVisualCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.INCORRECT_VISUAL,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_road_issue),
        carFeedbackIcon = RoadClosureCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ROAD_ISSUE,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_traffic_issue),
        carFeedbackIcon = TrafficIssueCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.TRAFFIC_ISSUE,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_positioning_issue),
        carFeedbackIcon = PositioningIssueCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.POSITIONING_ISSUE,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_other_issue),
        carFeedbackIcon = OtherIssueCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.OTHER_ISSUE,
    ),
)

fun buildFreeDriveFeedbackItemsProvider(carContext: CarContext): CarFeedbackItemProvider {
    return object : CarFeedbackItemProvider {
        override fun feedbackItems(): List<CarFeedbackItem> {
            return buildFreeDriveFeedbackItems(carContext)
        }
    }
}

// Pulled from FeedbackHelper.getActiveNavigationFeedbackTypes()
fun buildActiveGuidanceCarFeedbackItems(carContext: CarContext) = listOf(
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_incorrect_visual),
        carFeedbackIcon = IncorrectVisualCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.INCORRECT_VISUAL_GUIDANCE,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_incorrect_audio),
        carFeedbackIcon = ConfusingAudioCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.INCORRECT_AUDIO_GUIDANCE,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_routing_error),
        carFeedbackIcon = RoutingErrorCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ROUTING_ERROR,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_route_not_allowed),
        carFeedbackIcon = RouteNotAllowedCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ROUTE_NOT_ALLOWED,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_road_closed),
        carFeedbackIcon = RoadClosureCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ROAD_CLOSED,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_positioning_issue),
        carFeedbackIcon = PositioningIssueCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.POSITIONING_ISSUE,
    ),
)

fun activeGuidanceCarFeedbackProvider(carContext: CarContext): CarFeedbackItemProvider {
    return object : CarFeedbackItemProvider {
        override fun feedbackItems(): List<CarFeedbackItem> {
            return buildActiveGuidanceCarFeedbackItems(carContext)
        }
    }
}

// Pulled from FeedbackHelper.getActiveNavigationFeedbackTypes()
// This also mixes deep-link and search feedback. Poor routes may come from search!
fun buildRoutePreviewCarFeedbackItems(carContext: CarContext) = listOf(
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_road_closed),
        carFeedbackIcon = RoadClosureCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ROAD_CLOSED
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_positioning_issue),
        carFeedbackIcon = PositioningIssueCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.POSITIONING_ISSUE,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_routing_error),
        carFeedbackIcon = RoutingErrorCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ROUTING_ERROR,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_route_not_allowed),
        carFeedbackIcon = RouteNotAllowedCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ROUTE_NOT_ALLOWED,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_search_incorrect_location),
        carFeedbackIcon = IncorrectLocationCarFeedbackIcon,
        searchFeedbackReason = INCORRECT_LOCATION,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_other_issue),
        carFeedbackIcon = OtherIssueCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.OTHER_ISSUE,
    ),
)

fun routePreviewCarFeedbackProvider(carContext: CarContext): CarFeedbackItemProvider {
    return object : CarFeedbackItemProvider {
        override fun feedbackItems(): List<CarFeedbackItem> {
            return buildRoutePreviewCarFeedbackItems(carContext)
        }
    }
}

// Pulled from @com.mapbox.search.analytics.FeedbackEvent.FeedbackReason
fun buildSearchPlacesCarFeedbackItems(
    carContext: CarContext,
    favoriteRecords: List<FavoriteRecord>? = null,
    searchSuggestions: List<SearchSuggestion>? = null,
    geoDeeplink: GeoDeeplink? = null,
    geocodingResponse: GeocodingResponse? = null
) = listOf(
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_search_incorrect_address),
        carFeedbackIcon = IncorrectAddressCarFeedbackIcon,
        searchFeedbackReason = INCORRECT_ADDRESS,
        favoriteRecords = favoriteRecords,
        searchSuggestions = searchSuggestions,
        geoDeeplink = geoDeeplink,
        geocodingResponse = geocodingResponse,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_search_incorrect_location),
        carFeedbackIcon = IncorrectLocationCarFeedbackIcon,
        searchFeedbackReason = INCORRECT_LOCATION,
        favoriteRecords = favoriteRecords,
        searchSuggestions = searchSuggestions,
        geoDeeplink = geoDeeplink,
        geocodingResponse = geocodingResponse,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_search_incorrect_name),
        carFeedbackIcon = IncorrectNameCarFeedbackIcon,
        searchFeedbackReason = INCORRECT_NAME,
        favoriteRecords = favoriteRecords,
        searchSuggestions = searchSuggestions,
        geoDeeplink = geoDeeplink,
        geocodingResponse = geocodingResponse,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_search_other),
        carFeedbackIcon = SearchOtherIssueCarFeedbackIcon,
        searchFeedbackReason = OTHER,
        favoriteRecords = favoriteRecords,
        searchSuggestions = searchSuggestions,
        geoDeeplink = geoDeeplink,
        geocodingResponse = geocodingResponse,
    ),
)

fun buildSearchPlacesCarFeedbackProvider(
    carContext: CarContext,
    favoriteRecords: List<FavoriteRecord>? = null,
    searchSuggestions: List<SearchSuggestion>? = null,
    geoDeeplink: GeoDeeplink? = null,
    geocodingResponse: GeocodingResponse? = null
): CarFeedbackItemProvider {
    return object : CarFeedbackItemProvider {
        override fun feedbackItems(): List<CarFeedbackItem> {
            return buildSearchPlacesCarFeedbackItems(
                carContext,
                favoriteRecords,
                searchSuggestions,
                geoDeeplink,
                geocodingResponse
            )
        }
    }
}

fun buildArrivalFeedbackProvider(
    carContext: CarContext,
) = listOf(
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(
            R.string.car_feedback_positive_arrived_at_destination
        ),
        carFeedbackIcon = PositiveCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ARRIVAL_FEEDBACK_GOOD,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_positive_no_issue),
        carFeedbackIcon = PositiveCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ARRIVAL_FEEDBACK_GOOD,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_positive_amazing),
        carFeedbackIcon = PositiveCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ARRIVAL_FEEDBACK_GOOD,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_unable_arrive),
        carFeedbackIcon = NegativeCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ARRIVAL_FEEDBACK_NOT_GOOD,
    ),
    CarFeedbackItem(
        carFeedbackTitle = carContext.getString(R.string.car_feedback_negative_other_issue),
        carFeedbackIcon = NegativeCarFeedbackIcon,
        navigationFeedbackType = FeedbackEvent.ARRIVAL_FEEDBACK_NOT_GOOD,
    )
)
