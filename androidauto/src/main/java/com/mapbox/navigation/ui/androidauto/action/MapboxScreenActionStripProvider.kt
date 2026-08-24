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
        val arrivalOnClickListener = OnClickListener {
            val carArrivalTrigger = MapboxNavigationApp.getObservers(CarArrivalTrigger::class)
                .firstOrNull()
            checkNotNull(carArrivalTrigger) {
                "The CarArrivalTrigger must be attached while in active guidance."
            }
            carArrivalTrigger.triggerArrival()
        }
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
