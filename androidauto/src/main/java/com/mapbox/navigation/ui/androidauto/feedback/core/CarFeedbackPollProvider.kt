package com.mapbox.navigation.ui.androidauto.feedback.core

import androidx.annotation.DrawableRes
import androidx.car.app.CarContext
import androidx.car.app.model.CarIcon
import androidx.core.graphics.drawable.IconCompat
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackIcon
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackOption
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackPoll
import com.mapbox.search.analytics.FeedbackEvent.FeedbackReason

/**
 * Each screen contains contextual feedback options. This class contains default
 * items, override the functions to provide custom feedback options.
 */
open class CarFeedbackPollProvider {

    // Pulled from FeedbackHelper.getFreeDriveFeedbackTypes()
    open fun getFreeDriveFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        val options = listOf(
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_incorrect_visual),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_incorrect_visual),
                type = FeedbackEvent.INCORRECT_VISUAL,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_road_issue),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_road_closure),
                type = FeedbackEvent.ROAD_ISSUE,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_traffic_issue),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_traffic_issue),
                type = FeedbackEvent.TRAFFIC_ISSUE,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_positioning_issue),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_positioning_issue),
                type = FeedbackEvent.POSITIONING_ISSUE,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_other_issue),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_other_issue),
                type = FeedbackEvent.OTHER_ISSUE,
            ),
        )
        return CarFeedbackPoll(carContext.getString(R.string.car_feedback_title), options)
    }

    // Pulled from FeedbackHelper.getActiveNavigationFeedbackTypes()
    open fun getActiveGuidanceFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        val options = listOf(
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_incorrect_visual),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_incorrect_visual),
                type = FeedbackEvent.INCORRECT_VISUAL_GUIDANCE,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_incorrect_audio),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_confusing_audio),
                type = FeedbackEvent.INCORRECT_AUDIO_GUIDANCE,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_routing_error),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_routing_error),
                type = FeedbackEvent.ROUTING_ERROR,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_route_not_allowed),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_route_not_allowed),
                type = FeedbackEvent.ROUTE_NOT_ALLOWED,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_road_closed),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_road_closure),
                type = FeedbackEvent.ROAD_CLOSED,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_positioning_issue),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_positioning_issue),
                type = FeedbackEvent.POSITIONING_ISSUE,
            ),
        )
        return CarFeedbackPoll(carContext.getString(R.string.car_feedback_title), options)
    }

    // Pulled from FeedbackHelper.getActiveNavigationFeedbackTypes()
    // This also mixes deep-link and search feedback. Poor routes may come from search!
    open fun getRoutePreviewFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        val options = listOf(
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_road_closed),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_road_closure),
                type = FeedbackEvent.ROAD_CLOSED,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_positioning_issue),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_positioning_issue),
                type = FeedbackEvent.POSITIONING_ISSUE,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_routing_error),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_routing_error),
                type = FeedbackEvent.ROUTING_ERROR,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_route_not_allowed),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_route_not_allowed),
                type = FeedbackEvent.ROUTE_NOT_ALLOWED,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_search_incorrect_location),
                icon = carContext.getCarIcon(
                    R.drawable.mapbox_search_sdk_ic_feedback_reason_incorrect_location,
                ),
                searchFeedbackReason = FeedbackReason.INCORRECT_LOCATION,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_negative_other_issue),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_other_issue),
                type = FeedbackEvent.OTHER_ISSUE,
            ),
        )
        return CarFeedbackPoll(carContext.getString(R.string.car_feedback_title), options)
    }

    // Pulled from @com.mapbox.search.analytics.FeedbackEvent.FeedbackReason
    open fun getSearchFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        val options = listOf(
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_search_incorrect_address),
                icon = carContext.getCarIcon(
                    R.drawable.mapbox_search_sdk_ic_feedback_reason_incorrect_address,
                ),
                searchFeedbackReason = FeedbackReason.INCORRECT_ADDRESS,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_search_incorrect_location),
                icon = carContext.getCarIcon(
                    R.drawable.mapbox_search_sdk_ic_feedback_reason_incorrect_location,
                ),
                searchFeedbackReason = FeedbackReason.INCORRECT_LOCATION,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_search_incorrect_name),
                icon = carContext.getCarIcon(
                    R.drawable.mapbox_search_sdk_ic_feedback_reason_incorrect_name,
                ),
                searchFeedbackReason = FeedbackReason.INCORRECT_NAME,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_search_other),
                icon = carContext.getCarIcon(R.drawable.mapbox_search_sdk_ic_three_dots),
                searchFeedbackReason = FeedbackReason.OTHER,
            ),
        )
        return CarFeedbackPoll(carContext.getString(R.string.car_feedback_title), options)
    }

    open fun getArrivalFeedbackPoll(carContext: CarContext): CarFeedbackPoll {
        val options = listOf(
            CarFeedbackOption(
                title = carContext.getString(
                    R.string.car_feedback_arrival_positive,
                ),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_positive),
                type = FeedbackEvent.ARRIVAL_FEEDBACK_GOOD,
            ),
            CarFeedbackOption(
                title = carContext.getString(R.string.car_feedback_arrival_negative),
                icon = carContext.getCarIcon(R.drawable.mapbox_car_ic_feedback_negative),
                type = FeedbackEvent.ARRIVAL_FEEDBACK_NOT_GOOD,
            ),
        )
        return CarFeedbackPoll(carContext.getString(R.string.car_feedback_title), options)
    }

    private fun CarContext.getCarIcon(@DrawableRes iconId: Int): CarFeedbackIcon {
        val icon = IconCompat.createWithResource(this, iconId)
        return CarFeedbackIcon.Local(CarIcon.Builder(icon).build())
    }
}
