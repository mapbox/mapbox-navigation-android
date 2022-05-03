package com.mapbox.androidauto.car.feedback.ui

import androidx.annotation.DrawableRes
import com.mapbox.androidauto.R

sealed class CarFeedbackIcon(val name: String)

// Navigation feedback icons
object IncorrectVisualCarFeedbackIcon : CarFeedbackIcon("incorrect_visual")
object RoadClosureCarFeedbackIcon : CarFeedbackIcon("road_closure")
object PositioningIssueCarFeedbackIcon : CarFeedbackIcon("positioning_issue")
object TrafficIssueCarFeedbackIcon : CarFeedbackIcon("traffic_issue")
object RouteNotAllowedCarFeedbackIcon : CarFeedbackIcon("route_not_allowed")
object RoutingErrorCarFeedbackIcon : CarFeedbackIcon("routing_error")
object ConfusingAudioCarFeedbackIcon : CarFeedbackIcon("confusing_audio")
object OtherIssueCarFeedbackIcon : CarFeedbackIcon("other_issue")

// Search feedback icons
object IncorrectNameCarFeedbackIcon : CarFeedbackIcon("incorrect_name")
object IncorrectAddressCarFeedbackIcon : CarFeedbackIcon("incorrect_address")
object IncorrectLocationCarFeedbackIcon : CarFeedbackIcon("incorrect_location")
object SearchOtherIssueCarFeedbackIcon : CarFeedbackIcon("other_issue")

// Generic feedback icons
object PositiveCarFeedbackIcon : CarFeedbackIcon("thumbs_up")
object NegativeCarFeedbackIcon : CarFeedbackIcon("thumbs_down")

@SuppressWarnings("ComplexMethod") // no, it's simple :)
@DrawableRes
fun CarFeedbackIcon.drawableRes() = when (this) {
    // Navigation feedback icons
    IncorrectVisualCarFeedbackIcon -> R.drawable.mapbox_car_ic_feedback_incorrect_visual
    RoadClosureCarFeedbackIcon -> R.drawable.mapbox_car_ic_feedback_road_closure
    PositioningIssueCarFeedbackIcon -> R.drawable.mapbox_car_ic_feedback_positioning_issue
    TrafficIssueCarFeedbackIcon -> R.drawable.mapbox_car_ic_feedback_traffic_issue
    RouteNotAllowedCarFeedbackIcon -> R.drawable.mapbox_car_ic_feedback_route_not_allowed
    RoutingErrorCarFeedbackIcon -> R.drawable.mapbox_car_ic_feedback_routing_error
    ConfusingAudioCarFeedbackIcon -> R.drawable.mapbox_car_ic_feedback_confusing_audio
    OtherIssueCarFeedbackIcon -> R.drawable.mapbox_car_ic_feedback_other_issue

    // Search feedback icons
    IncorrectNameCarFeedbackIcon -> R.drawable.mapbox_search_sdk_ic_feedback_reason_incorrect_name
    IncorrectAddressCarFeedbackIcon -> R.drawable.mapbox_search_sdk_ic_feedback_reason_incorrect_address
    IncorrectLocationCarFeedbackIcon -> R.drawable.mapbox_search_sdk_ic_feedback_reason_incorrect_location
    SearchOtherIssueCarFeedbackIcon -> R.drawable.mapbox_search_sdk_ic_three_dots

    // Generic feedback icons
    PositiveCarFeedbackIcon -> R.drawable.mapbox_car_ic_feedback_positive
    NegativeCarFeedbackIcon -> R.drawable.mapbox_car_ic_feedback_negative
}
