package com.mapbox.navigation.route.internal.offboard

import android.content.Context
import com.mapbox.api.directions.v5.MapboxDirections
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.api.directionsrefresh.v1.MapboxDirectionsRefresh
import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.base.internal.accounts.UrlSkuTokenProvider
import com.mapbox.navigation.base.route.RouteRefreshCallback
import com.mapbox.navigation.base.route.RouteRefreshError
import com.mapbox.navigation.base.route.Router
import com.mapbox.navigation.base.route.RouterCallback
import com.mapbox.navigation.base.route.RouterFailure
import com.mapbox.navigation.base.route.RouterOrigin
import com.mapbox.navigation.route.internal.util.ACCESS_TOKEN_QUERY_PARAM
import com.mapbox.navigation.route.internal.util.redactQueryParam
import com.mapbox.navigation.route.offboard.RouteBuilderProvider
import com.mapbox.navigation.route.offboard.routerefresh.RouteRefreshCallbackMapper
import com.mapbox.navigation.utils.internal.RequestMap
import com.mapbox.navigation.utils.internal.cancelRequest
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

/**
 * MapboxOffboardRouter provides online route-fetching
 *
 * @param accessToken mapboxAccessToken token
 * @param context application [Context]
 * @param urlSkuTokenProvider [UrlSkuTokenProvider]
 */
class MapboxOffboardRouter(
    private val accessToken: String,
    private val context: Context,
    private val urlSkuTokenProvider: UrlSkuTokenProvider
) : Router {

    private companion object {
        private val TAG = Tag("MbxOffboardRouter")
        private const val ROUTES_LIST_EMPTY = "routes list is empty"
        private const val UNKNOWN = "unknown"
    }

    private val directionRequests = RequestMap<MapboxDirections>()
    private val refreshRequests = RequestMap<MapboxDirectionsRefresh>()

    /**
     * Fetch routes based on [RouteOptions].
     *
     * @param routeOptions RouteOptions
     * @param callback Callback that gets notified with the results of the request
     *
     * @return request ID, can be used to cancel the request with [cancelAll]
     */
    override fun getRoute(
        routeOptions: RouteOptions,
        callback: RouterCallback
    ): Long {
        val mapboxDirections = RouteBuilderProvider
            .getBuilder(urlSkuTokenProvider)
            .accessToken(accessToken)
            .routeOptions(routeOptions)
            .build()
        val requestId = directionRequests.put(mapboxDirections)
        mapboxDirections.enqueueCall(
            object : Callback<DirectionsResponse> {
                override fun onResponse(
                    call: Call<DirectionsResponse>,
                    response: Response<DirectionsResponse>
                ) {
                    directionRequests.remove(requestId)
                    val urlWithoutToken = call.request().url.redactQueryParam(
                        ACCESS_TOKEN_QUERY_PARAM
                    ).toUrl()
                    val routes = response.body()?.routes()
                    val code = response.code()
                    when {
                        call.isCanceled -> callback.onCanceled(
                            routeOptions, RouterOrigin.Offboard
                        )
                        response.isSuccessful -> {
                            if (!routes.isNullOrEmpty()) {
                                callback.onRoutesReady(routes, RouterOrigin.Offboard)
                            } else {
                                callback.onFailure(
                                    listOf(
                                        RouterFailure(
                                            urlWithoutToken,
                                            RouterOrigin.Offboard,
                                            ROUTES_LIST_EMPTY,
                                            code
                                        )
                                    ),
                                    routeOptions
                                )
                            }
                        }
                        else -> callback.onFailure(
                            listOf(
                                RouterFailure(
                                    urlWithoutToken,
                                    RouterOrigin.Offboard,
                                    response.errorBody()?.string().toString(),
                                    code
                                )
                            ),
                            routeOptions
                        )
                    }
                }

                override fun onFailure(call: Call<DirectionsResponse>, t: Throwable) {
                    directionRequests.remove(requestId)
                    if (call.isCanceled) {
                        callback.onCanceled(routeOptions, RouterOrigin.Offboard)
                    } else {
                        callback.onFailure(
                            listOf(
                                RouterFailure(
                                    url = call.request().url.redactQueryParam(
                                        ACCESS_TOKEN_QUERY_PARAM
                                    ).toUrl(),
                                    routerOrigin = RouterOrigin.Offboard,
                                    message = t.message ?: UNKNOWN,
                                    code = null,
                                    throwable = t
                                )
                            ),
                            routeOptions
                        )
                    }
                }
            }
        )
        return requestId
    }

    /**
     * Cancels a specific route request.
     *
     * @see [getRoute]
     */
    override fun cancelRouteRequest(requestId: Long) {
        directionRequests.cancelRequest(requestId, TAG) {
            it.cancelCall()
        }
    }

    /**
     * Interrupts a route-fetching request if one is in progress.
     */
    override fun cancelAll() {
        directionRequests.removeAll().forEach {
            it.cancelCall()
        }
        refreshRequests.removeAll().forEach {
            it.cancelCall()
        }
    }

    /**
     * Release used resources.
     */
    override fun shutdown() {
        cancelAll()
    }

    /**
     * Refresh the traffic annotations for a given [DirectionsRoute]
     *
     * @param route DirectionsRoute the direction route to refresh
     * @param legIndex Int the index of the current leg in the route
     * @param callback Callback that gets notified with the results of the request
     */
    override fun getRouteRefresh(
        route: DirectionsRoute,
        legIndex: Int,
        callback: RouteRefreshCallback
    ): Long {
        val routeOptions = route.routeOptions()
        val mapboxDirectionsRefresh = RouteBuilderProvider.getRefreshBuilder()
            .accessToken(accessToken)
            .baseUrl(routeOptions?.baseUrl())
            .requestId(route.requestUuid())
            .routeIndex(route.routeIndex()?.toIntOrNull() ?: 0)
            .legIndex(legIndex)
            .interceptor {
                val httpUrl = (it.request() as Request).url
                val skuUrl = urlSkuTokenProvider.obtainUrlWithSkuToken(httpUrl.toUrl())
                it.proceed(it.request().newBuilder().url(skuUrl).build())
            }
            .build()
        val requestId = refreshRequests.put(mapboxDirectionsRefresh)

        mapboxDirectionsRefresh.enqueueCall(object : Callback<DirectionsRefreshResponse> {
            override fun onResponse(
                call: Call<DirectionsRefreshResponse>,
                response: Response<DirectionsRefreshResponse>
            ) {
                refreshRequests.remove(requestId)
                if (response.isSuccessful) {
                    val routeAnnotations = response.body()?.route()
                    var errorThrowable: Throwable? = null
                    val refreshedDirectionsRoute = try {
                        RouteRefreshCallbackMapper.mapToDirectionsRoute(route, routeAnnotations)
                    } catch (t: Throwable) {
                        errorThrowable = t
                        null
                    }
                    if (refreshedDirectionsRoute != null) {
                        callback.onRefresh(refreshedDirectionsRoute)
                    } else {
                        callback.onError(
                            RouteRefreshError(
                                message = "Failed to read refresh response",
                                throwable = errorThrowable ?: Exception(
                                    "Message=[${response.message()}]; " +
                                        "url = [${(call.request() as Request).url}]" +
                                        "errorBody = [${response.errorBody()}];" +
                                        "refresh route = [$routeAnnotations]"
                                )
                            )
                        )
                    }
                } else {
                    callback.onError(
                        RouteRefreshError(
                            message = "Route refresh failed",
                            throwable = Exception(
                                "Message=[${response.message()}]; " +
                                    "url = [${(call.request() as Request).url}]" +
                                    "code = [${response.code()}];" +
                                    "errorBody = [${response.errorBody()}];"
                            )
                        )
                    )
                }
            }

            override fun onFailure(call: Call<DirectionsRefreshResponse>, t: Throwable) {
                refreshRequests.remove(requestId)
                callback.onError(
                    RouteRefreshError(
                        "Route refresh failed; " +
                            "url = [${(call.request() as Request).url}]",
                        throwable = t
                    )
                )
            }
        })
        return requestId
    }

    /**
     * Cancels a specific route refresh request.
     *
     * @see [getRouteRefresh]
     */
    override fun cancelRouteRefreshRequest(requestId: Long) {
        refreshRequests.cancelRequest(requestId, TAG) {
            it.cancelCall()
        }
    }
}
