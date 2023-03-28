package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.extension.style.expressions.generated.Expression
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.maps.plugin.gestures.OnMapClickListener
import com.mapbox.maps.plugin.gestures.gestures
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.RouteLineFeaturesActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.route.line.MapboxRouteLineApiExtensions.findClosestRoute
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedCallback
import com.mapbox.navigation.ui.maps.route.line.api.RoutesRenderedResult
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineScaleValue
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor
import com.mapbox.navigation.utils.internal.ifNonNull
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class RouteLineFeaturesActivity : AppCompatActivity() {

    private val routeClickPadding = com.mapbox.android.gestures.Utils.dpToPx(30f)

    private val viewBinding: RouteLineFeaturesActivityLayoutBinding by lazy {
        RouteLineFeaturesActivityLayoutBinding.inflate(layoutInflater)
    }

    private val mapboxMap: MapboxMap by lazy {
        viewBinding.mapView.getMapboxMap()
    }

    private val routeTrafficLineScaleExpression: Expression by lazy {
        buildScalingExpression(
            listOf(
                RouteLineScaleValue(4f, 3f, .5f),
                RouteLineScaleValue(10f, 4f, .5f),
                RouteLineScaleValue(13f, 6f, .5f),
                RouteLineScaleValue(16f, 10f, .5f),
                RouteLineScaleValue(19f, 14f, .5f),
                RouteLineScaleValue(22f, 18f, .5f)
            )
        )
    }

    private val routeLineColorResources by lazy {
        RouteLineColorResources.Builder()
            .routeUnknownCongestionColor(Color.CYAN)
            .alternativeRouteCasingColor(Color.parseColor("#179C6A"))
            .alternativeRouteDefaultColor(Color.parseColor("#17E899"))
            .alternativeRouteUnknownCongestionColor(Color.parseColor("#F5AC00"))
            .alternativeRouteLowCongestionColor(Color.parseColor("#FF9A00"))
            .alternativeRouteModerateCongestionColor(Color.parseColor("#E8720C"))
            .alternativeRouteHeavyCongestionColor(Color.parseColor("#FF5200"))
            .alternativeRouteSevereCongestionColor(Color.parseColor("#F52B00"))
            .build()
    }

    private val routeLineResources: RouteLineResources by lazy {
        RouteLineResources.Builder()
            .routeLineColorResources(routeLineColorResources)
            .routeTrafficLineScaleExpression(routeTrafficLineScaleExpression)
            .alternativeRouteTrafficLineScaleExpression(routeTrafficLineScaleExpression)
            .build()
    }

    private val routeLineOptions: MapboxRouteLineOptions by lazy {
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(routeLineResources)
            .withRouteLineBelowLayerId("road-label-navigation")
            .withRouteStyleDescriptors(
                listOf(
                    RouteStyleDescriptor("altRoute1", Color.CYAN, Color.MAGENTA),
                )
            )
            .build()
    }

    private val routeLineView by lazy {
        MapboxRouteLineView(routeLineOptions)
    }

    private val routeLineApi: MapboxRouteLineApi by lazy {
        MapboxRouteLineApi(routeLineOptions)
    }

    private val routesRenderedCallback = object : RoutesRenderedCallback {

        override fun onRoutesRendered(result: RoutesRenderedResult) {
            Log.i(LOG_CATEGORY, "Routes rendered: $result")
        }
    }

    private val routesClearedCallback = object : RoutesRenderedCallback {

        override fun onRoutesRendered(result: RoutesRenderedResult) {
            Log.i(LOG_CATEGORY, "Routes cleared: $result")
        }
    }

    private var showTraffic = true
    private var showPrimaryRoute = true
    private var showAltRoutes = true
    private var routesAreShowing = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initStyle()
        initListeners()
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun initListeners() {
        viewBinding.btnToggleTrafficVisibility.setOnClickListener {
            when (showTraffic) {
                true -> routeLineView.hideTraffic(mapboxMap.getStyle()!!)
                false -> routeLineView.showTraffic(mapboxMap.getStyle()!!)
            }
            showTraffic = !showTraffic
        }

        viewBinding.btnTogglePrimaryRouteVisibility.setOnClickListener {
            when (showPrimaryRoute) {
                true -> routeLineView.hidePrimaryRoute(mapboxMap.getStyle()!!)
                false -> routeLineView.showPrimaryRoute(mapboxMap.getStyle()!!)
            }
            showPrimaryRoute = !showPrimaryRoute
        }

        viewBinding.btnToggleAlternativeRouteVisibility.setOnClickListener {
            when (showAltRoutes) {
                true -> routeLineView.hideAlternativeRoutes(mapboxMap.getStyle()!!)
                false -> routeLineView.showAlternativeRoutes(mapboxMap.getStyle()!!)
            }
            showAltRoutes = !showAltRoutes
        }

        viewBinding.btnClear.setOnClickListener {
            when (routesAreShowing) {
                true -> {
                    routeLineApi.clearRouteLine {
                        routeLineView.renderClearRouteLineValue(
                            mapboxMap.getStyle()!!,
                            it,
                            mapboxMap,
                            routesClearedCallback
                        )
                    }
                    viewBinding.btnClear.text = "Draw Routes"
                }
                false -> {
                    drawRoutes()
                    viewBinding.btnClear.text = "Clear Routes"
                }
            }
            routesAreShowing = !routesAreShowing
        }
    }

    @SuppressLint("MissingPermission")
    private fun initStyle() {
        mapboxMap.loadStyleUri(
            "mapbox://styles/mapbox/light-v10",
            { style: Style ->

                val route1 = getRoute(R.raw.basic_route4)
                val routeOrigin = Utils.getRouteOriginPoint(route1)
                val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(12.0).build()
                viewBinding.mapView.getMapboxMap().setCamera(cameraOptions)

                drawRoutes()
            },
            object : OnMapLoadErrorListener {
                override fun onMapLoadError(eventData: MapLoadingErrorEventData) {
                    Log.e(
                        CustomAlternativeRouteColoringActivity::class.java.simpleName,
                        "Error loading map - error type: " +
                            "${eventData.type}, message: ${eventData.message}"
                    )
                }
            }
        )

        viewBinding.mapView.gestures.addOnMapClickListener(mapClickListener)
    }

    @OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
    private fun drawRoutes() {
        ifNonNull(mapboxMap.getStyle()) { style ->
            val route1 = getRoute(R.raw.basic_route4)
            val route2 = getRoute(R.raw.basic_route5)
            val route3 = getRoute(R.raw.basic_route6)
            routeLineApi.setRoutes(
                listOf(
                    RouteLine(route1, null),
                    RouteLine(route2, null),
                    RouteLine(route3, "altRoute1")
                )
            ) {
                routeLineView.renderRouteDrawData(style, it, mapboxMap, routesRenderedCallback)
            }
        }
    }

    private fun getRoute(routeResourceId: Int): DirectionsRoute {
        val routeAsString = Utils.readRawFileText(this, routeResourceId)
        return DirectionsRoute.fromJson(routeAsString)
    }

    private fun buildScalingExpression(scalingValues: List<RouteLineScaleValue>): Expression {
        val expressionBuilder = Expression.ExpressionBuilder("interpolate")
        expressionBuilder.addArgument(Expression.exponential { literal(1.5) })
        expressionBuilder.zoom()
        scalingValues.forEach { routeLineScaleValue ->
            expressionBuilder.stop {
                this.literal(routeLineScaleValue.scaleStop.toDouble())
                product {
                    literal(routeLineScaleValue.scaleMultiplier.toDouble())
                    literal(routeLineScaleValue.scale.toDouble())
                }
            }
        }
        return expressionBuilder.build()
    }

    private val mapClickListener = OnMapClickListener {
        CoroutineScope(Dispatchers.Main).launch {
            val result = routeLineApi.findClosestRoute(
                it,
                mapboxMap,
                routeClickPadding
            )

            val routeFound = result.value?.route
            if (routeFound != null && routeFound != routeLineApi.getPrimaryRoute()) {
                val reOrderedRoutes = routeLineApi.getRoutes()
                    .filter { it != routeFound }
                    .toMutableList()
                    .also {
                        it.add(0, routeFound)
                    }.mapIndexed { index, directionsRoute ->
                        val identifier = if (index < 2) {
                            null
                        } else {
                            "altRoute1"
                        }
                        RouteLine(directionsRoute, identifier)
                    }
                routeLineApi.setRoutes(reOrderedRoutes) {
                    routeLineView.renderRouteDrawData(mapboxMap.getStyle()!!, it)
                }
            }
        }
        false
    }

    private companion object {
        private const val LOG_CATEGORY = "[RouteLineFeatures]"
    }
}
