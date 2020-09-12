package com.mapbox.navigation.ui.route

import android.graphics.drawable.Drawable
import com.mapbox.maps.Style
import com.mapbox.maps.plugin.style.expressions.Expression
import com.mapbox.maps.plugin.style.expressions.Expression.Companion.color
import com.mapbox.maps.plugin.style.expressions.Expression.Companion.switchCase
import com.mapbox.maps.plugin.style.expressions.dsl.eq
import com.mapbox.maps.plugin.style.expressions.dsl.interpolate
import com.mapbox.maps.plugin.style.expressions.dsl.match
import com.mapbox.maps.plugin.style.layers.LineLayer
import com.mapbox.maps.plugin.style.layers.SymbolLayer
import com.mapbox.maps.plugin.style.layers.properties.IconPitchAlignment
import com.mapbox.maps.plugin.style.layers.properties.LineCap
import com.mapbox.maps.plugin.style.layers.properties.LineJoin
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.ALTERNATIVE_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER
import com.mapbox.navigation.ui.internal.route.RouteConstants.DESTINATION_MARKER_NAME
import com.mapbox.navigation.ui.internal.route.RouteConstants.ORIGIN_MARKER_NAME
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_CASING_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.PRIMARY_ROUTE_TRAFFIC_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_DESTINATION_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_LAYER_ID
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_ORIGIN_VALUE
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_PROPERTY_KEY
import com.mapbox.navigation.ui.internal.route.RouteConstants.WAYPOINT_SOURCE_ID
import com.mapbox.navigation.ui.internal.route.RouteLayerProvider
import com.mapbox.navigation.ui.internal.utils.MapImageUtils
import kotlin.reflect.KProperty1

internal interface MapboxRouteLayerProvider : RouteLayerProvider {
    val routeStyleDescriptors: List<RouteStyleDescriptor>

    fun getRouteLineColorExpressions(
        defaultColor: Int,
        routeColorProvider: KProperty1<RouteStyleDescriptor, Int>
    ): List<Expression> {
        val expressions = mutableListOf<Expression>(
            eq {
                get { DEFAULT_ROUTE_DESCRIPTOR_PLACEHOLDER }
                literal(true)
            },
            color(defaultColor)
        )
        routeStyleDescriptors.forEach {
            expressions.add(eq {
                get { it.routeIdentifier }
                literal(true)
            })
            expressions.add(color(routeColorProvider.get(it)))
        }
        return expressions.plus(color(defaultColor))
    }

    fun getRouteLineWidthExpressions(scale: Double): Expression {
        return interpolate {
            exponential { literal(1.5) }
            zoom()
            stop {
               literal(4.0)
               product {
                   literal(3.0)
                   literal(scale)
               }
            }
            stop {
                literal(10.0)
                product {
                    literal(4.0)
                    literal(scale)
                }
            }
            stop {
                literal(13.0)
                product {
                    literal(6.0)
                    literal(scale)
                }
            }
            stop {
                literal(16.0)
                product {
                    literal(10.0)
                    literal(scale)
                }
            }
            stop {
                literal(19.0)
                product {
                    literal(14.0)
                    literal(scale)
                }
            }
            stop {
                literal(22.0)
                product {
                    literal(18.0)
                    literal(scale)
                }
            }
        }
    }

    fun getCasingLineWidthExpression(scale: Double): Expression {
        return interpolate {
            exponential {
                literal(1.5)
            }
            zoom()
            stop {
                literal(10.0)
                literal(7.0)
            }
            stop {
                literal(14.0)
                product {
                    literal(10.5)
                    literal(scale)
                }
            }
            stop {
                literal(16.5)
                product {
                    literal(15.5)
                    literal(scale)
                }
            }
            stop {
                literal(19.0)
                product {
                    literal(24.0)
                    literal(scale)
                }
            }
            stop {
                literal(22.0)
                product {
                    literal(29.0)
                    literal(scale)
                }
            }
        }
    }

    override fun initializePrimaryRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        scale: Double,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = getRouteLineWidthExpressions(scale)
        val routeLineColorExpressions =
            getRouteLineColorExpressions(color, RouteStyleDescriptor::lineColorResourceId)
        return initializeRouteLayer(
            style,
            roundedLineCap,
            PRIMARY_ROUTE_LAYER_ID,
            PRIMARY_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions
        )
    }

    override fun initializePrimaryRouteTrafficLayer(
        style: Style,
        roundedLineCap: Boolean,
        scale: Double,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = getRouteLineWidthExpressions(scale)
        val routeLineColorExpressions =
            getRouteLineColorExpressions(color, RouteStyleDescriptor::lineColorResourceId)
        return initializeRouteLayer(
            style,
            roundedLineCap,
            PRIMARY_ROUTE_TRAFFIC_LAYER_ID,
            PRIMARY_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions
        )
    }

    override fun initializePrimaryRouteCasingLayer(
        style: Style,
        scale: Double,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = getCasingLineWidthExpression(scale)
        val routeLineColorExpressions =
            getRouteLineColorExpressions(color, RouteStyleDescriptor::lineShieldColorResourceId)
        return initializeRouteLayer(
            style,
            true,
            PRIMARY_ROUTE_CASING_LAYER_ID,
            PRIMARY_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions
        )
    }

    override fun initializeAlternativeRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        scale: Double,
        color: Int
    ): LineLayer {
        val lineWidthExpression = getRouteLineWidthExpressions(scale)
        val routeLineColorExpressions =
            getRouteLineColorExpressions(color, RouteStyleDescriptor::lineColorResourceId)
        return initializeRouteLayer(
            style,
            roundedLineCap,
            ALTERNATIVE_ROUTE_LAYER_ID,
            ALTERNATIVE_ROUTE_SOURCE_ID,
            lineWidthExpression,
            routeLineColorExpressions
        )
    }

    override fun initializeAlternativeRouteCasingLayer(
        style: Style,
        scale: Double,
        color: Int
    ): LineLayer {
        val lineWidthScaleExpression = getCasingLineWidthExpression(scale)
        val routeLineColorExpressions =
            getRouteLineColorExpressions(color, RouteStyleDescriptor::lineShieldColorResourceId)
        return initializeRouteLayer(
            style,
            true,
            ALTERNATIVE_ROUTE_CASING_LAYER_ID,
            ALTERNATIVE_ROUTE_SOURCE_ID,
            lineWidthScaleExpression,
            routeLineColorExpressions
        )
    }

    override fun initializeWayPointLayer(
        style: Style,
        originIcon: Drawable,
        destinationIcon: Drawable
    ): SymbolLayer {
        if (style.layerExists(WAYPOINT_LAYER_ID)) {
            style.removeLayer(WAYPOINT_LAYER_ID)
        }

        MapImageUtils.getBitmapFromDrawable(originIcon).let {
            style.addImage(ORIGIN_MARKER_NAME, it)
        }

        MapImageUtils.getBitmapFromDrawable(destinationIcon).let {
            style.addImage(DESTINATION_MARKER_NAME, it)
        }

        return SymbolLayer(WAYPOINT_LAYER_ID, WAYPOINT_SOURCE_ID)
            .iconImage(
                match {
                    toString {
                        get { literal(WAYPOINT_PROPERTY_KEY) }
                    }
                    literal(ORIGIN_MARKER_NAME)
                    stop {
                        WAYPOINT_ORIGIN_VALUE
                        literal(ORIGIN_MARKER_NAME)
                    }
                    stop {
                        WAYPOINT_DESTINATION_VALUE
                        literal(DESTINATION_MARKER_NAME)
                    }
                }
            )
            .iconSize(
                interpolate {
                    exponential { literal(1.5) }
                    zoom()
                    stop {
                        literal(0.0)
                        literal(0.6)
                    }
                    stop {
                        literal(10.0)
                        literal(0.8)
                    }
                    stop {
                        literal(12.0)
                        literal(1.3)
                    }
                    stop {
                        literal(22.0)
                        literal(2.8)
                    }
                }
            )
            .iconPitchAlignment(IconPitchAlignment.MAP)
            .iconAllowOverlap(true)
            .iconIgnorePlacement(true)
    }

    private fun initializeRouteLayer(
        style: Style,
        roundedLineCap: Boolean,
        layerId: String,
        layerSourceId: String,
        lineWidthExpression: Expression,
        colorExpressions: List<Expression>
    ): LineLayer {
        if (style.layerExists(layerId)) {
            style.removeLayer(layerId)
        }

        val lineCapValue = when (roundedLineCap) {
            true -> LineCap.ROUND
            false -> LineCap.BUTT
        }

        val lineJoinValue = when (roundedLineCap) {
            true -> LineJoin.ROUND
            false -> LineJoin.BEVEL
        }

        return LineLayer(layerId, layerSourceId)
            .lineCap(lineCapValue)
            .lineJoin(lineJoinValue)
            .lineWidth(lineWidthExpression)
            .lineColor(
                switchCase(*colorExpressions.toTypedArray())
            )
    }
}
