@file:JvmName("CachedNavigationFeedbackEventEx")
package com.mapbox.navigation.core.internal.utils

import com.mapbox.navigation.core.internal.telemetry.CachedNavigationFeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent

fun CachedNavigationFeedbackEvent.hasDetailedFeedback(): Boolean =
    this.feedbackType != FeedbackEvent.POSITIONING_ISSUE

fun List<CachedNavigationFeedbackEvent>.hasDetailedFeedbackItems(): Boolean =
    this.any { it.hasDetailedFeedback() }
