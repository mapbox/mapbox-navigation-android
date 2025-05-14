package com.mapbox.navigation.ui.maps.internal.route.line

import androidx.annotation.Keep
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import com.mapbox.bindgen.Value
import com.mapbox.geojson.FeatureCollection
import com.mapbox.geojson.Point
import com.mapbox.geojson.PointAsCoordinatesTypeAdapter
import com.mapbox.maps.StylePropertyValue
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.extension.style.layers.properties.generated.IconAnchor
import com.mapbox.maps.extension.style.layers.properties.generated.IconPitchAlignment
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.trip.model.RouteProgressState
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineApiOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleExpressions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineTrimOffset
import com.mapbox.navigation.ui.maps.route.model.FadingConfig

@Keep
abstract class RouteLineEvent(
    @SerializedName(Keys.KEY_SUBTYPE)
    val subtype: String,
    val instanceId: String,
) {

    fun toJson(): String {
        val gsonBuilder = GsonBuilder()
        registerAllTypeAdapters(gsonBuilder)
        return gsonBuilder.create().toJson(this)
    }

    companion object {

        private fun registerAllTypeAdapters(gsonBuilder: GsonBuilder) {
            // MapboxRouteLineApiOptions
            gsonBuilder.registerTypeAdapter(IntRange::class.java, IntRangeTypeAdapter())
            // updateTraveledRouteLine
            gsonBuilder.registerTypeAdapter(Point::class.java, PointAsCoordinatesTypeAdapter())
            // RouteLineDynamicData
            gsonBuilder.registerTypeAdapter(Expression::class.java, ExpressionTypeAdapter())
            // RouteLineData
            gsonBuilder.registerTypeAdapter(
                FeatureCollection::class.java,
                FeatureCollectionAdapter(),
            )
        }
    }
}

@Keep
class RouteLineApiEvent(
    instanceId: String,
    val value: RouteLineApiEventValue,
) : RouteLineEvent(Keys.SUBTYPE_API, instanceId)

@Keep
class RouteLineViewEvent(
    instanceId: String,
    val value: RouteLineViewEventValue,
) : RouteLineEvent(Keys.SUBTYPE_VIEW, instanceId)

@Keep
abstract class RouteLineApiEventValue(
    @SerializedName(Keys.KEY_ACTION)
    val action: String,
)

@Keep
class RouteLineApiOptionsEventValue(
    val options: MapboxRouteLineApiOptions,
) : RouteLineApiEventValue(Keys.ACTION_OPTIONS)

@Keep
class RouteLineApiSetRoutesValue(
    val legIndex: Int,
    val routeLines: List<LightRouteLine>,
) : RouteLineApiEventValue(Keys.ACTION_SET_ROUTES)

@Keep
data class LightRouteLine(
    val routeId: String,
    val featureId: String?,
)

@Keep
class RouteLineApiUpdateTraveledRouteLineValue(
    val point: Point,
) : RouteLineApiEventValue(Keys.ACTION_UPDATE_TRAVELED_ROUTE_LINE)

@Keep
class RouteLineApiClearRouteLineValue :
    RouteLineApiEventValue(Keys.ACTION_CLEAR_ROUTE_LINE)

@Keep
class RouteLineApiSetVanishingOffsetValue(
    val offset: Double,
) : RouteLineApiEventValue(Keys.ACTION_SET_VANISHING_OFFSET)

@Keep
class RouteLineApiUpdateWithRouteProgressValue(
    val routeId: String,
    val routeGeometryIndex: Int,
    val state: RouteProgressState,
    val legIndex: Int?,
) : RouteLineApiEventValue(Keys.ACTION_UPDATE_WITH_ROUTE_PROGRESS)

@Keep
class RouteLineApiCancelValue : RouteLineApiEventValue(Keys.ACTION_CANCEL)

@Keep
abstract class RouteLineViewEventValue(
    @SerializedName(Keys.KEY_ACTION)
    val action: String,
)

@Keep
abstract class RouteLineViewWithStyleIdValue(
    action: String,
    val styleId: String?,
) : RouteLineViewEventValue(action)

@Keep
class RouteLineViewInitialOptionsValue(
    val initialOptions: RouteLineViewOptionsData,
) : RouteLineViewEventValue(Keys.ACTION_INITIAL_OPTIONS)

@Keep
class RouteLineViewPushUpdateDynamicOptionsValue(
    styleId: String?,
    val newOptions: RouteLineViewOptionsData,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_UPDATE_DYNAMIC_OPTIONS, styleId)

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Keep
data class RouteLineViewOptionsData(
    val routeLineColorResources: RouteLineColorResources,
    val scaleExpressions: RouteLineScaleExpressions,
    val restrictedRoadDashArray: List<Double>,
    val restrictedRoadOpacity: Double,
    val restrictedRoadLineWidth: Double,
    val displaySoftGradientForTraffic: Boolean,
    val softGradientTransition: Double,
    val originIconId: Int,
    val destinationIconId: Int,
    val waypointLayerIconOffset: List<Double>,
    val waypointLayerIconAnchor: IconAnchor,
    val iconPitchAlignment: IconPitchAlignment,
    val displayRestrictedRoadSections: Boolean,
    val routeLineBelowLayerId: String?,
    val tolerance: Double,
    val shareLineGeometrySources: Boolean,
    val lineDepthOcclusionFactor: Double,
    val slotName: String,
    val fadeOnHighZoomsConfig: FadingConfig?,
    val routeLineBlurWidth: Double,
    val routeLineBlurEnabled: Boolean,
    val applyTrafficColorsToRouteLineBlur: Boolean,
    val routeLineBlurOpacity: Double,
)

@Keep
class RouteLineViewInitializeLayersValue(
    styleId: String?,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_INITIALIZE_LAYERS, styleId)

@Keep
class RouteLineViewRenderRouteDrawDataValue(
    styleId: String?,
    val input: RouteLineViewExpectedInput,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_RENDER_ROUTE_DRAW_DATA, styleId)

@Keep
class RouteLineViewRenderRouteLineUpdateValue(
    styleId: String?,
    val input: RouteLineViewExpectedInput,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_RENDER_ROUTE_LINE_UPDATE, styleId)

@Keep
class RouteLineViewRenderRouteLineClearValue(
    styleId: String?,
    val input: RouteLineViewExpectedInput,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_RENDER_ROUTE_LINE_CLEAR_VALUE, styleId)

@Keep
class RouteLineViewShowPrimaryRouteValue(
    styleId: String?,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_SHOW_PRIMARY_ROUTE, styleId)

@Keep
class RouteLineViewHidePrimaryRouteValue(
    styleId: String?,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_HIDE_PRIMARY_ROUTE, styleId)

@Keep
class RouteLineViewShowAlternativeRoutesValue(
    styleId: String?,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_SHOW_ALTERNATIVE_ROUTES, styleId)

@Keep
class RouteLineViewHideAlternativeRoutesValue(
    styleId: String?,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_HIDE_ALTERNATIVE_ROUTES, styleId)

@Keep
class RouteLineViewShowTrafficValue(
    styleId: String?,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_SHOW_TRAFFIC, styleId)

@Keep
class RouteLineViewHideTrafficValue(
    styleId: String?,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_HIDE_TRAFFIC, styleId)

@Keep
class RouteLineViewShowOriginAndDestinationValue(
    styleId: String?,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_SHOW_ORIGIN_AND_DESTINATION, styleId)

@Keep
class RouteLineViewHideOriginAndDestinationValue(
    styleId: String?,
) : RouteLineViewWithStyleIdValue(Keys.ACTION_HIDE_ORIGIN_AND_DESTINATION, styleId)

@Keep
class RouteLineViewCancelValue : RouteLineViewEventValue(Keys.ACTION_CANCEL)

@Keep
abstract class RouteLineViewExpectedInput(
    @SerializedName(Keys.KEY_TYPE)
    val type: String,
)

@Keep
data class RouteLineViewDataError(
    val message: String,
) : RouteLineViewExpectedInput(Keys.TYPE_ERROR)

@Keep
data class RouteLineViewRenderRouteDrawDataInputValue(
    val primaryRouteLineData: RouteLineEventData,
    val alternativeRouteLinesData: List<RouteLineEventData>,
    val waypointsSource: FeatureCollection,
    val routeLineMaskingLayerDynamicData: RouteLineDynamicEventData?,
) : RouteLineViewExpectedInput(Keys.TYPE_VALUE_RENDER_ROUTE_DRAW_DATA)

@Keep
data class RouteLineViewRenderRouteLineUpdateDataValue(
    val primaryRouteLineDynamicData: RouteLineDynamicEventData?,
    val alternativeRouteLinesDynamicData: List<RouteLineDynamicEventData>,
    val routeLineMaskingLayerDynamicData: RouteLineDynamicEventData?,
) : RouteLineViewExpectedInput(Keys.TYPE_VALUE_RENDER_ROUTE_LINE_UPDATE)

@Keep
data class RouteLineViewRenderRouteLineClearDataValue(
    val primaryRouteSource: FeatureCollection,
    val alternativeRoutesSources: List<FeatureCollection>,
    val waypointsSource: FeatureCollection,
) : RouteLineViewExpectedInput(Keys.TYPE_VALUE_RENDER_ROUTE_LINE_CLEAR)

@Keep
data class RouteLineEventData(
    val featureCollection: FeatureCollection,
    val dynamicData: RouteLineDynamicEventData?,
)

@Keep
data class RouteLineDynamicEventData(
    val baseExpressionData: RouteLineExpressionEventData,
    val casingExpressionData: RouteLineExpressionEventData,
    val trafficExpressionData: RouteLineExpressionEventData?,
    val restrictedSectionExpressionData: RouteLineExpressionEventData?,
    val trimOffset: RouteLineTrimOffset?,
    val trailExpressionData: RouteLineExpressionEventData?,
    val trailCasingExpressionData: RouteLineExpressionEventData?,
    val blurExpressionCommandData: RouteLineExpressionEventData?,
)

@Keep
abstract class RouteLineExpressionEventData(
    @SerializedName(Keys.KEY_TYPE)
    val type: String,
)

@Keep
class RouteLineNoOpExpressionEventData : RouteLineExpressionEventData(Keys.TYPE_NO_OP) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        return true
    }

    override fun hashCode(): Int {
        return javaClass.hashCode()
    }
}

@Keep
data class RouteLineProviderBasedExpressionEventData(
    val property: String,
    @Deprecated("Used for backwards compatibility")
    val expression: Expression? = null,
    val value: StylePropertyValue? = null,
) : RouteLineExpressionEventData(Keys.TYPE_PROVIDER_BASED)

object Keys {

    const val SUBTYPE_API = "api"
    const val SUBTYPE_VIEW = "view"

    const val ACTION_OPTIONS = "options"
    const val ACTION_SET_ROUTES = "set_routes"
    const val ACTION_UPDATE_TRAVELED_ROUTE_LINE = "update_traveled_route_line"
    const val ACTION_SET_VANISHING_OFFSET = "set_vanishing_offset"
    const val ACTION_CLEAR_ROUTE_LINE = "clear_route_line"
    const val ACTION_UPDATE_WITH_ROUTE_PROGRESS = "update_with_route_progress"
    const val ACTION_CANCEL = "cancel"
    const val ACTION_INITIAL_OPTIONS = "initial_options"
    const val ACTION_UPDATE_DYNAMIC_OPTIONS = "update_dynamic_options"
    const val ACTION_INITIALIZE_LAYERS = "initialize_layers"
    const val ACTION_RENDER_ROUTE_DRAW_DATA = "render_route_draw_data"
    const val ACTION_RENDER_ROUTE_LINE_UPDATE = "render_route_line_update"
    const val ACTION_RENDER_ROUTE_LINE_CLEAR_VALUE = "render_route_line_clear_value"
    const val ACTION_SHOW_PRIMARY_ROUTE = "show_primary_route"
    const val ACTION_HIDE_PRIMARY_ROUTE = "hide_primary_route"
    const val ACTION_SHOW_ALTERNATIVE_ROUTES = "show_alternative_routes"
    const val ACTION_HIDE_ALTERNATIVE_ROUTES = "hide_alternative_routes"
    const val ACTION_SHOW_TRAFFIC = "show_traffic"
    const val ACTION_HIDE_TRAFFIC = "hide_traffic"
    const val ACTION_SHOW_ORIGIN_AND_DESTINATION = "show_origin_and_destination"
    const val ACTION_HIDE_ORIGIN_AND_DESTINATION = "hide_origin_and_destination"

    const val TYPE_NO_OP = "no_op"
    const val TYPE_PROVIDER_BASED = "provider_based"
    const val TYPE_ERROR = "error"
    const val TYPE_VALUE_RENDER_ROUTE_DRAW_DATA = "value_render_route_draw_data"
    const val TYPE_VALUE_RENDER_ROUTE_LINE_UPDATE = "value_render_route_line_update"
    const val TYPE_VALUE_RENDER_ROUTE_LINE_CLEAR = "value_render_route_line_clear"

    const val KEY_SUBTYPE = "subtype"
    const val KEY_ACTION = "action"
    const val KEY_TYPE = "type"
}
