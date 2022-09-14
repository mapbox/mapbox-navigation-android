package com.mapbox.navigation.qa_test_app.view.dropin

import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AlertDialog
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.base.route.NavigationRoute
import com.mapbox.navigation.base.route.NavigationRouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.attachCreated
import com.mapbox.navigation.core.internal.extensions.flowNewRawLocation
import com.mapbox.navigation.dropin.NavigationViewApi
import com.mapbox.navigation.dropin.NavigationViewListener
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityNavigationViewBinding
import com.mapbox.navigation.qa_test_app.databinding.LayoutDrawerMenuNavViewBinding
import com.mapbox.navigation.qa_test_app.view.base.DrawerActivity
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import com.mapbox.navigation.utils.internal.toPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class MapboxNavigationViewInActiveGuidanceActivity : DrawerActivity() {

    private lateinit var binding: LayoutActivityNavigationViewBinding
    private lateinit var menuBinding: LayoutDrawerMenuNavViewBinding

    override fun onCreateContentView(): View {
        binding = LayoutActivityNavigationViewBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onCreateMenuView(): View {
        menuBinding = LayoutDrawerMenuNavViewBinding.inflate(layoutInflater)
        return menuBinding.root
    }

    private var dialog: AlertDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        menuBinding.toggleReplay.isChecked = binding.navigationView.api.isReplayEnabled()
        menuBinding.toggleReplay.setOnCheckedChangeListener { _, isChecked ->
            binding.navigationView.api.routeReplayEnabled(isChecked)
        }

        val destination = Point.fromLngLat(-77.0361813, 38.89992)
        dialog = AlertDialog.Builder(this)
            .setCancelable(true)
            .setOnCancelListener {
                finish()
            }
            .setTitle("Please wait...")
            .setMessage("Starting navigation")
            .show()

        binding.navigationView.addListener(object: NavigationViewListener() {
            private var inActiveNavigation = false

            override fun onFreeDrive() {
                super.onFreeDrive()
                // NavigationView transitions back to FreeDrive state once
                // End Navigation button is pressed.
                if (inActiveNavigation) {
                    finish()
                }
            }

            override fun onActiveNavigation() {
                super.onActiveNavigation()
                inActiveNavigation = true
                dialog?.dismiss()
                dialog = null
            }

            override fun onBackPressed(): Boolean {
                finish()
                return true
            }
        })

        attachCreated(FetchRouteAndStartNavigation(binding.navigationView.api, destination))
    }

    private class FetchRouteAndStartNavigation(
        private val api: NavigationViewApi,
        private val destination: Point
    ) : UIComponent() {
        override fun onAttached(mapboxNavigation: MapboxNavigation) {
            super.onAttached(mapboxNavigation)

            coroutineScope.launch {
                val origin = mapboxNavigation.currentLocation()
                val routes = mapboxNavigation.fetchRoute(origin, destination)
                api.startActiveGuidance(routes)
            }
        }
    }
}

private suspend fun MapboxNavigation.currentLocation() =
    flowNewRawLocation().take(1).first().toPoint()

private suspend fun MapboxNavigation.fetchRoute(
    origin: Point,
    destination: Point
): List<NavigationRoute> {
    val routeOptions =
        RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(navigationOptions.applicationContext)
            .layersList(listOf(getZLevel(), null))
            .coordinatesList(listOf(origin, destination))
            .alternatives(true)
            .build()

    return suspendCancellableCoroutine { cont ->
        val requestId = requestRoutes(
            routeOptions,
            object : NavigationRouterCallback {
                override fun onRoutesReady(
                    routes: List<NavigationRoute>,
                    routerOrigin: RouterOrigin
                ) {
                    cont.resume(routes)
                }

                override fun onFailure(
                    reasons: List<RouterFailure>,
                    routeOptions: RouteOptions
                ) {
                    cont.resumeWithException(FetchRouteError(reasons, routeOptions))
                }

                override fun onCanceled(
                    routeOptions: RouteOptions,
                    routerOrigin: RouterOrigin
                ) {
                    cont.cancel(FetchRouteCancelled(routeOptions, routerOrigin))
                }
            }
        )
        cont.invokeOnCancellation { cancelRouteRequest(requestId) }
    }
}

private class FetchRouteError(
    val reasons: List<RouterFailure>,
    val routeOptions: RouteOptions
) : Error()

private class FetchRouteCancelled(
    val routeOptions: RouteOptions,
    val routerOrigin: RouterOrigin
) : Error()
