package com.mapbox.navigation.ui.maps.route.line

import com.mapbox.geojson.Point
import com.mapbox.navigation.base.trip.model.RouteProgress
import com.mapbox.navigation.ui.maps.internal.route.line.LightRouteLine
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineApiCancelValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineApiClearRouteLineValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineApiEvent
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineApiOptionsEventValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineApiSetRoutesValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineApiSetVanishingOffsetValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineApiUpdateTraveledRouteLineValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineApiUpdateWithRouteProgressValue
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.NavigationRouteLine

internal class RouteLineHistoryRecordingApiSender : RouteLineHistoryRecordingInstance() {

    fun sendOptionsEvent(options: MapboxRouteLineApiOptions) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventOrAddToQueue {
            RouteLineApiEvent(
                instanceId,
                RouteLineApiOptionsEventValue(options),
            )
        }
    }

    fun sendSetRoutesEvent(routeLines: List<NavigationRouteLine>, legIndex: Int) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineApiEvent(
                instanceId,
                RouteLineApiSetRoutesValue(
                    legIndex,
                    routeLines.map { LightRouteLine(it.route.id, it.identifier) },
                ),
            )
        }
    }

    fun sendUpdateTraveledRouteLineEvent(point: Point) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineApiEvent(
                instanceId,
                RouteLineApiUpdateTraveledRouteLineValue(point),
            )
        }
    }

    fun sendClearRouteLineEvent() {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineApiEvent(instanceId, RouteLineApiClearRouteLineValue())
        }
    }

    fun sendSetVanishingOffsetEvent(offset: Double) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineApiEvent(instanceId, RouteLineApiSetVanishingOffsetValue(offset))
        }
    }

    fun sendUpdateWithRouteProgressEvent(routeProgress: RouteProgress) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineApiEvent(
                instanceId,
                RouteLineApiUpdateWithRouteProgressValue(
                    routeProgress.navigationRoute.id,
                    routeProgress.currentRouteGeometryIndex,
                    routeProgress.currentState,
                    routeProgress.currentLegProgress?.legIndex,
                ),
            )
        }
    }

    fun sendCancelEvent() {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineApiEvent(instanceId, RouteLineApiCancelValue())
        }
    }
}
