@file:JvmName("ConstantsEx")

package com.mapbox.navigation.utils

const val NOTIFICATION_CHANNEL = "Navigation Notifications"
const val NAVIGATION_NOTIFICATION_CHANNEL = "NAVIGATION_NOTIFICATION_CHANNEL"
const val END_NAVIGATION_ACTION = "com.mapbox.intent.action.END_NAVIGATION"
const val SET_BACKGROUND_COLOR = "setBackgroundColor"
const val NOTIFICATION_ID = 7654

// TODO wait for NavigationConstants will be merged
/**
 * Defines the minimum zoom level of the displayed map.
 */
const val NAVIGATION_MINIMUM_MAP_ZOOM = 7.0

/**
 * Duration in which the AlertView is shown with the "Report Problem" text.
 */
const val ALERT_VIEW_PROBLEM_DURATION: Long = 10000

/**
 * Duration in which the feedback BottomSheet is shown.
 */
const val FEEDBACK_BOTTOM_SHEET_DURATION: Long = 10000
