package com.mapbox.androidauto.car

import androidx.car.app.Screen
import com.mapbox.androidauto.ArrivalState
import com.mapbox.androidauto.FreeDriveState
import com.mapbox.androidauto.MapboxCarApp
import com.mapbox.androidauto.RoutePreviewState
import com.mapbox.androidauto.car.feedback.core.CarFeedbackSender
import com.mapbox.androidauto.car.feedback.ui.CarFeedbackAction
import com.mapbox.androidauto.car.feedback.ui.CarGridFeedbackScreen
import com.mapbox.androidauto.car.feedback.ui.activeGuidanceCarFeedbackProvider
import com.mapbox.androidauto.car.feedback.ui.buildArrivalFeedbackProvider
import com.mapbox.androidauto.car.navigation.ActiveGuidanceScreen
import com.mapbox.androidauto.car.navigation.CarActiveGuidanceCarContext
import com.mapbox.androidauto.car.permissions.NeedsLocationPermissionsScreen
import com.mapbox.androidauto.car.preview.CarRoutePreviewScreen
import com.mapbox.androidauto.car.preview.RoutePreviewCarContext
import com.mapbox.androidauto.navigation.audioguidance.CarAudioGuidanceUi
import com.mapbox.maps.MapboxExperimental
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Open class which allows you to override any screen with your own customized screen.
 *
 * Please create an issue in github, requesting the customization for the screen you're looking
 * to modify. https://github.com/mapbox/mapbox-navigation-android/issues/new
 */
@MapboxExperimental
@ExperimentalPreviewMapboxNavigationAPI
open class MapboxScreenProvider(val mainCarContext: MainCarContext) {

    /**
     * The screen shown when location permissions have not been accepted.
     */
    open fun needsLocationPermission(): Screen {
        return NeedsLocationPermissionsScreen(mainCarContext.carContext)
    }

    /**
     * The screen shown the [MapboxCarApp.carAppState] is in [FreeDriveState]
     */
    open fun freeDriveScreen(): Screen {
        return MainCarScreen(mainCarContext)
    }

    /**
     * The screen shown the [MapboxCarApp.carAppState] is in [RoutePreviewState]
     */
    open fun routePreviewScreen(state: RoutePreviewState): Screen {
        val routePreviewCarContext = RoutePreviewCarContext(mainCarContext)
        return CarRoutePreviewScreen(routePreviewCarContext, state.placeRecord, state.routes)
    }

    /**
     * The screen shown the [MapboxCarApp.carAppState] is in [ActiveGuidanceScreen]
     */
    open fun activeGuidanceScreen(): Screen {
        return ActiveGuidanceScreen(
            CarActiveGuidanceCarContext(mainCarContext),
            listOf(
                CarFeedbackAction(
                    mainCarContext.mapboxCarMap,
                    CarFeedbackSender(),
                    activeGuidanceCarFeedbackProvider(mainCarContext.carContext),
                ),
                CarAudioGuidanceUi(),
            ),
        )
    }

    /**
     * The screen shown the [MapboxCarApp.carAppState] is in [ArrivalState]. This screen moves the
     * app to [FreeDriveState] when it is completed.
     */
    open fun arrivalScreen(): Screen {
        return CarGridFeedbackScreen(
            mainCarContext.carContext,
            javaClass.simpleName,
            CarFeedbackSender(),
            feedbackItems = buildArrivalFeedbackProvider(mainCarContext.carContext),
            encodedSnapshot = null,
        ) {
            mainCarContext.mapboxNavigation.setNavigationRoutes(emptyList())
            MapboxCarApp.updateCarAppState(FreeDriveState)
        }
    }
}
