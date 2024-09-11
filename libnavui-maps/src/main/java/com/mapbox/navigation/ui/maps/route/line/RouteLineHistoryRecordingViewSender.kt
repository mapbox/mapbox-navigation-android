package com.mapbox.navigation.ui.maps.route.line

import com.mapbox.bindgen.Expected
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewCancelValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewDataError
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewEvent
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewHideAlternativeRoutesValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewHideOriginAndDestinationValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewHidePrimaryRouteValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewHideTrafficValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewInitialOptionsValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewInitializeLayersValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewOptionsData
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewPushUpdateDynamicOptionsValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewRenderRouteDrawDataValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewRenderRouteLineClearValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewRenderRouteLineUpdateValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewShowAlternativeRoutesValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewShowOriginAndDestinationValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewShowPrimaryRouteValue
import com.mapbox.navigation.ui.maps.internal.route.line.RouteLineViewShowTrafficValue
import com.mapbox.navigation.ui.maps.internal.route.line.toEventValue
import com.mapbox.navigation.ui.maps.internal.route.line.toInput
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineClearValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineError
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineUpdateValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteSetValue

internal class RouteLineHistoryRecordingViewSender : RouteLineHistoryRecordingInstance() {

    private var options: RouteLineViewOptionsData? = null

    fun sendInitialOptionsEvent(data: RouteLineViewOptionsData) {
        options = data.also { data ->
            RouteLineHistoryRecordingPusherProvider.instance.pushEventOrAddToQueue {
                RouteLineViewEvent(
                    instanceId,
                    RouteLineViewInitialOptionsValue(data),
                )
            }
        }
    }

    fun sendUpdateDynamicOptionsEvent(styleId: String?, data: RouteLineViewOptionsData) {
        options = data.also { data ->
            RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
                RouteLineViewEvent(
                    instanceId,
                    RouteLineViewPushUpdateDynamicOptionsValue(styleId, data),
                )
            }
        }
    }

    fun sendInitializeLayersEvent(styleId: String?) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(
                instanceId,
                RouteLineViewInitializeLayersValue(styleId),
            )
        }
    }

    fun sendRenderRouteDrawDataEvent(
        styleId: String?,
        value: Expected<RouteLineError, RouteSetValue>,
    ) {
        options.also { data ->
            RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
                RouteLineViewEvent(
                    instanceId,
                    RouteLineViewRenderRouteDrawDataValue(
                        styleId,
                        value.toInput {
                            if (data == null) {
                                RouteLineViewDataError("NoOptions")
                            } else {
                                toEventValue(data)
                            }
                        },
                    ),
                )
            }
        }
    }

    fun sendRenderRouteLineUpdateEvent(
        styleId: String?,
        value: Expected<RouteLineError, RouteLineUpdateValue>,
    ) {
        options.also { data ->
            if (value.isValue) {
                RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
                    RouteLineViewEvent(
                        instanceId,
                        RouteLineViewRenderRouteLineUpdateValue(
                            styleId,
                            value.toInput {
                                if (data == null) {
                                    RouteLineViewDataError("NoOptions")
                                } else {
                                    toEventValue(data)
                                }
                            },
                        ),
                    )
                }
            }
        }
    }

    fun sendClearRouteLineValueEvent(
        styleId: String?,
        value: Expected<RouteLineError, RouteLineClearValue>,
    ) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(
                instanceId,
                RouteLineViewRenderRouteLineClearValue(
                    styleId,
                    value.toInput { toEventValue() },
                ),
            )
        }
    }

    fun sendShowPrimaryRouteEvent(styleId: String?) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(instanceId, RouteLineViewShowPrimaryRouteValue(styleId))
        }
    }

    fun sendHidePrimaryRouteEvent(styleId: String?) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(instanceId, RouteLineViewHidePrimaryRouteValue(styleId))
        }
    }

    fun sendShowAlternativeRoutesEvent(styleId: String?) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(instanceId, RouteLineViewShowAlternativeRoutesValue(styleId))
        }
    }

    fun sendHideAlternativeRoutesEvent(styleId: String?) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(instanceId, RouteLineViewHideAlternativeRoutesValue(styleId))
        }
    }

    fun sendHideTrafficEvent(styleId: String?) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(instanceId, RouteLineViewHideTrafficValue(styleId))
        }
    }

    fun sendShowTrafficEvent(styleId: String?) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(instanceId, RouteLineViewShowTrafficValue(styleId))
        }
    }

    fun sendShowOriginAndDestinationPointsEvent(styleId: String?) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(
                instanceId,
                RouteLineViewShowOriginAndDestinationValue(styleId),
            )
        }
    }

    fun sendHideOriginAndDestinationPointsEvent(styleId: String?) {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(instanceId, RouteLineViewHideOriginAndDestinationValue(styleId))
        }
    }

    fun sendCancelEvent() {
        RouteLineHistoryRecordingPusherProvider.instance.pushEventIfEnabled {
            RouteLineViewEvent(instanceId, RouteLineViewCancelValue())
        }
    }
}
