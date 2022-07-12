package com.mapbox.androidauto

/**
 * These states are communicated between the car and app.
 *
 * For example, when the car changes the app state to [ActiveGuidanceState]
 *   The app's MainActivity will update to show the ActiveGuidanceFragment
 *   The car's MainCarSession will update to show the ActiveGuidanceScreen
 */
sealed class CarAppState
object FreeDriveState : CarAppState()
object RoutePreviewState : CarAppState()
object ActiveGuidanceState : CarAppState()
object ArrivalState : CarAppState()
