package com.mapbox.navigation.route.offboard.test

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.coroutineScope
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.geojson.Point
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.internal.extensions.applyDefaultParams
import com.mapbox.navigation.base.internal.route.RouteUrl
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.route.offboard.R
import com.mapbox.navigation.route.offboard.internal.MapboxOffboardRouter
import kotlin.coroutines.suspendCoroutine
import kotlinx.coroutines.launch

class OffboardRouterTestActivity : AppCompatActivity() {

    private lateinit var router: Router
    private val token: String by lazy { getString(R.string.mapbox_access_token) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_offboard_router_test)

        initRouter()
    }

    private fun initRouter() {
        val dummyTokenProvider = object : UrlSkuTokenProvider {
            override fun obtainUrlWithSkuToken(resourceUrl: String, querySize: Int): String {
                return "$resourceUrl&sku=123456"
            }
        }

        router = MapboxOffboardRouter(token, this, dummyTokenProvider)
    }

    fun checkRouteFetching() {
        lifecycle.coroutineScope.launch {
            val result = fetchRoute()
            sendResult(result)
        }
    }

    private fun sendResult(result: Int) {
        val intent = Intent().apply {
            putExtra(ROUTE_RESULT, result)
        }
        setResult(Activity.RESULT_OK, intent)
        finish()
    }

    private suspend fun fetchRoute(): Int =
        suspendCoroutine { cont ->
            val options = RouteOptions.builder().applyDefaultParams()
                .accessToken(token)
                .coordinates(
                    listOf(
                        Point.fromLngLat(ORIGIN_TEST_LON, ORIGIN_TEST_LAT),
                        Point.fromLngLat(DESTINATION_TEST_LON, DESTINATION_TEST_LAT)
                    )
                )
                .profile(RouteUrl.PROFILE_DRIVING_TRAFFIC)
                .build()

            router.getRoute(options, object : Router.Callback {
                override fun onResponse(routes: List<DirectionsRoute>) {
                    cont.resumeWith(Result.success(ROUTE_READY))
                }

                override fun onFailure(throwable: Throwable) {
                    cont.resumeWith(Result.success(ROUTE_FAILURE))
                }

                override fun onCanceled() {
                    cont.resumeWith(Result.success(ROUTE_CANCELED))
                }
            })
        }

    companion object {
        const val ROUTE_RESULT = "route_result"

        const val ROUTE_READY = 1
        const val ROUTE_FAILURE = -1
        const val ROUTE_CANCELED = -2

        private const val ORIGIN_TEST_LON = 1.1
        private const val ORIGIN_TEST_LAT = 2.2
        private const val DESTINATION_TEST_LON = 3.3
        private const val DESTINATION_TEST_LAT = 4.4
    }
}
