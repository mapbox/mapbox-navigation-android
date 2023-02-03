package com.mapbox.navigation.qa_test_app.view

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import androidx.annotation.ColorInt
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.MapboxMap
import com.mapbox.maps.Style
import com.mapbox.maps.extension.observable.eventdata.MapLoadingErrorEventData
import com.mapbox.maps.plugin.delegates.listeners.OnMapLoadErrorListener
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.databinding.CustomAlternativeRouteColoringActivityLayoutBinding
import com.mapbox.navigation.qa_test_app.utils.Utils
import com.mapbox.navigation.ui.maps.NavigationStyles
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineApi
import com.mapbox.navigation.ui.maps.route.line.api.MapboxRouteLineView
import com.mapbox.navigation.ui.maps.route.line.model.MapboxRouteLineOptions
import com.mapbox.navigation.ui.maps.route.line.model.RouteLine
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineColorResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteLineResources
import com.mapbox.navigation.ui.maps.route.line.model.RouteStyleDescriptor

class CustomAlternativeRouteColoringActivity : AppCompatActivity() {

    private val viewBinding: CustomAlternativeRouteColoringActivityLayoutBinding by lazy {
        CustomAlternativeRouteColoringActivityLayoutBinding.inflate(layoutInflater)
    }

    private val mapboxMap: MapboxMap by lazy {
        viewBinding.mapView.getMapboxMap()
    }

    private val styles = listOf(
        NavigationStyles.NAVIGATION_DAY_STYLE,
        NavigationStyles.NAVIGATION_NIGHT_STYLE,
        Style.LIGHT,
    )

    private val colors = listOf(
        RouteLineColorResources.Builder().build().routeDefaultColor,
        Color.GREEN,
    )

    private var routeColor = colors.first()
    private var styleId = styles.first()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(viewBinding.root)
        initStyle(styleId)
        viewBinding.buttonChangeStyle.setOnClickListener {
            styleId = styles.filter { it != styleId }.random()
            initStyle(styleId)
        }
        viewBinding.buttonChangePrimaryRouteColor.setOnClickListener {
            routeColor = colors.filter { it != routeColor }.random()
            setRoutes(mapboxMap.getStyle()!!, generateRouteLineOptions(styleId, routeColor))
        }
    }

    @SuppressLint("MissingPermission")
    private fun initStyle(
        styleId: String,
    ) {
        mapboxMap.loadStyleUri(
            styleId,
            { style: Style ->
                setRoutes(style, generateRouteLineOptions(styleId, routeColor))
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
    }

    private fun setRoutes(style: Style, options: MapboxRouteLineOptions) {
        val routeLineApi = MapboxRouteLineApi(options)
        val routeLineView = MapboxRouteLineView(options)
        val route1 = getRoute(R.raw.basic_route1)
        val route2 = getRoute(R.raw.basic_route2)
        val route3 = getRoute(R.raw.basic_route3)
        routeLineApi.setRoutes(
            listOf(
                RouteLine(route1, null),
                RouteLine(route2, "altRoute1"),
                RouteLine(route3, "altRoute2")
            )
        ) {
            routeLineView.renderRouteDrawData(style, it)
            routeLineView.hideTraffic(style)
        }
        val routeOrigin = Utils.getRouteOriginPoint(route1)
        val cameraOptions = CameraOptions.Builder().center(routeOrigin).zoom(14.0).build()
        viewBinding.mapView.getMapboxMap().setCamera(cameraOptions)
    }

    private fun generateRouteLineOptions(styleId: String, @ColorInt routeColor: Int) =
        MapboxRouteLineOptions.Builder(this)
            .withRouteLineResources(
                RouteLineResources.Builder()
                    .routeLineColorResources(
                        RouteLineColorResources.Builder()
                            .routeDefaultColor(routeColor)
                            .build()
                    )
                    .build()
            )
            .withRouteLineBelowLayerId(
                when (styleId) {
                    NavigationStyles.NAVIGATION_DAY_STYLE -> "road-label-navigation"
                    Style.LIGHT -> "road-label"
                    else -> "road-label-navigation"
                }
            )
            .withRouteStyleDescriptors(
                listOf(
                    RouteStyleDescriptor("altRoute1", Color.YELLOW, Color.GREEN),
                    RouteStyleDescriptor("altRoute2", Color.CYAN, Color.MAGENTA)
                )
            )
            .build()

    private fun getRoute(routeResourceId: Int): DirectionsRoute {
        val routeAsString = Utils.readRawFileText(this, routeResourceId)
        return DirectionsRoute.fromJson(routeAsString)
    }
}
