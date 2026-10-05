package com.mapbox.navigation.ui.androidauto.action

import androidx.car.app.Screen
import androidx.car.app.model.Action
import androidx.car.app.model.ActionStrip
import androidx.car.app.model.OnClickListener
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.ui.androidauto.R
import com.mapbox.navigation.ui.androidauto.feedback.ui.CarFeedbackAction
import com.mapbox.navigation.ui.androidauto.freedrive.FreeDriveActionStrip
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure
import com.mapbox.navigation.ui.androidauto.navigation.CarArrivalTrigger
import com.mapbox.navigation.ui.androidauto.navigation.audioguidance.CarAudioGuidanceAction
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreen
import com.mapbox.navigation.ui.androidauto.screenmanager.MapboxScreenManager

/**
 * Gives you the ability to override the [ActionStrip] some of the [MapboxScreen].
 *
 * Screens are entirely customizable with the [MapboxScreenManager], but this makes it possible to
 * customize the action buttons without replacing every [MapboxScreen].
 */
@ExperimentalPreviewMapboxNavigationAPI
open class MapboxScreenActionStripProvider {
    /**
     * The entry point for all action strip requests. Override this function when you want to
     * ensure your own actions are used for any screen with customizable actions.
     */
    open fun getActionStrip(screen: Screen, @MapboxScreen.Key mapboxScreen: String): ActionStrip {
        return when (mapboxScreen) {
            MapboxScreen.FREE_DRIVE -> getFreeDrive(screen)
            MapboxScreen.SEARCH -> getSearch(screen)
            MapboxScreen.FAVORITES -> getFavorites(screen)
            MapboxScreen.GEO_DEEPLINK -> getGeoDeeplink(screen)
            MapboxScreen.ROUTE_PREVIEW -> getRoutePreview(screen)
            MapboxScreen.ACTIVE_GUIDANCE -> getActiveGuidance(screen)
            else -> throw NotImplementedError(
                "The $mapboxScreen does not have customizable actions at this time.",
            )
        }
    }

    /**
     * Allows you to override the [MapboxScreen.FREE_DRIVE] [ActionStrip]
     */
    protected open fun getFreeDrive(screen: Screen): ActionStrip {
        return FreeDriveActionStrip(screen).builder().build()
    }

    /**
     * Allows you to override the [MapboxScreen.SEARCH] [ActionStrip]
     */
    protected open fun getSearch(screen: Screen): ActionStrip {
        return ActionStrip.Builder()
            .addAction(
                CarFeedbackAction(
                    MapboxScreen.SEARCH_FEEDBACK,
                ).getAction(screen),
            )
            .build()
    }

    /**
     * Allows you to override the [MapboxScreen.FAVORITES] [ActionStrip]
     */
    protected open fun getFavorites(screen: Screen): ActionStrip {
        return ActionStrip.Builder()
            .addAction(
                CarFeedbackAction(
                    MapboxScreen.FAVORITES_FEEDBACK,
                ).getAction(screen),
            )
            .build()
    }

    /**
     * Allows you to override the [MapboxScreen.GEO_DEEPLINK] [ActionStrip]
     */
    protected open fun getGeoDeeplink(screen: Screen): ActionStrip {
        return ActionStrip.Builder()
            .addAction(
                CarFeedbackAction(
                    MapboxScreen.GEO_DEEPLINK_FEEDBACK,
                ).getAction(screen),
            )
            .build()
    }

    /**
     * Allows you to override the [MapboxScreen.ROUTE_PREVIEW] [ActionStrip]
     */
    protected open fun getRoutePreview(screen: Screen): ActionStrip {
        return ActionStrip.Builder()
            .addAction(
                CarFeedbackAction(
                    MapboxScreen.ROUTE_PREVIEW_FEEDBACK,
                ).getAction(screen),
            )
            .build()
    }

    /**
     * Allows you to override the [MapboxScreen.ACTIVE_GUIDANCE] [ActionStrip]
     */
    protected open fun getActiveGuidance(screen: Screen): ActionStrip {
        val arrivalOnClickListener = OnClickListener { triggerArrivalOnStop() }
        return ActionStrip.Builder()
            .addAction(CarFeedbackAction(MapboxScreen.ACTIVE_GUIDANCE_FEEDBACK).getAction(screen))
            .addAction(CarAudioGuidanceAction().getAction(screen))
            .addAction(
                Action.Builder()
                    .setTitle(
                        screen.carContext.getString(R.string.car_action_navigation_stop_button),
                    )
                    .setOnClickListener(arrivalOnClickListener).build(),
            )
            .build()
    }
}

/**
 * Shows the arrival screen when the driver presses Stop during active guidance.
 *
 * The [CarArrivalTrigger] is registered only while a guidance screen is resumed, so a click can
 * arrive without one. If a guidance screen is still on top, the arrival screen is shown anyway,
 * which is all the trigger does. Otherwise the click came from a guidance template that is no
 * longer shown, and it is ignored so it doesn't replace the screen the driver moved to.
 */
internal fun triggerArrivalOnStop() {
    val carArrivalTrigger = MapboxNavigationApp.getObservers(CarArrivalTrigger::class)
        .firstOrNull()
    val topScreen = MapboxScreenManager.current()?.key
    when {
        carArrivalTrigger != null -> carArrivalTrigger.triggerArrival()
        topScreen in GUIDANCE_SCREENS -> {
            logAndroidAutoFailure(
                "Stop pressed without an attached CarArrivalTrigger, showing the arrival screen",
            )
            MapboxScreenManager.replaceTop(MapboxScreen.ARRIVAL)
        }
        else -> logAndroidAutoFailure(
            "Stop ignored, the guidance screen is not on top (top screen: $topScreen)",
        )
    }
}

@Suppress("DEPRECATION")
private val GUIDANCE_SCREENS = setOf(MapboxScreen.NAVIGATION, MapboxScreen.ACTIVE_GUIDANCE)
