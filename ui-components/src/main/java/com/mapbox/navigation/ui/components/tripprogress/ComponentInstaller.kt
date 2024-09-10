package com.mapbox.navigation.ui.components.tripprogress

import android.content.Context
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.tripdata.progress.model.TripProgressUpdateFormatter
import com.mapbox.navigation.ui.base.installer.ComponentInstaller
import com.mapbox.navigation.ui.base.installer.Installation
import com.mapbox.navigation.ui.components.tripprogress.internal.ui.MapboxTripProgressComponentContract
import com.mapbox.navigation.ui.components.tripprogress.internal.ui.TripProgressComponent
import com.mapbox.navigation.ui.components.tripprogress.view.MapboxTripProgressView
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Install component that renders [MapboxTripProgressView].
 */
@ExperimentalPreviewMapboxNavigationAPI
fun ComponentInstaller.tripProgress(
    tripProgressView: MapboxTripProgressView,
    config: TripProgressConfig.() -> Unit = {},
): Installation {
    val componentConfig = TripProgressConfig(tripProgressView.context).apply(config)
    val contract = MapboxTripProgressComponentContract(componentConfig.tripOverviewRoutes)

    return component(
        TripProgressComponent(
            tripProgressView,
            { contract },
            componentConfig.tripProgressFormatter,
        ),
    )
}

/**
 * Trip progress component configuration class.
 */
@ExperimentalPreviewMapboxNavigationAPI
class TripProgressConfig internal constructor(context: Context) {
    /**
     * Flowable with routes list used to calculate trip details and rendering trip overview.
     * Emitting emptyList() disables trip overview.
     */
    var tripOverviewRoutes: Flow<List<NavigationRoute>> = flowOf(emptyList())

    /**
     * An instance of [TripProgressUpdateFormatter] used to create the [MapboxTripProgressApi]
     * used by the [TripProgressComponent].
     */
    var tripProgressFormatter: TripProgressUpdateFormatter =
        TripProgressUpdateFormatter.Builder(context).build()
}
