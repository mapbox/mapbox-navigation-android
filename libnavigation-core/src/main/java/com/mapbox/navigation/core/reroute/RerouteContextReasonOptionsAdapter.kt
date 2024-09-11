package com.mapbox.navigation.core.reroute

import com.google.gson.JsonPrimitive
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.core.internal.router.GetRouteSignature
import com.mapbox.navigation.utils.internal.logD
import com.mapbox.navigation.utils.internal.logE

private const val CONTEXT_QUERY_PARAM = "context"
private const val REASON_QUERY_PARAM = "reason"
private const val REROUTE_CONTEXT = "reroute"
private const val REROUTE_DEVIATION_REASON = "deviation"
private const val REROUTE_PARAMETERS_CHANGE_REASON = "parameters_change"
private const val LOG_TAG = "RerouteContextReasonOptionsAdapter"

internal class RerouteContextReasonOptionsAdapter : InternalRerouteOptionsAdapter {

    override fun onRouteOptions(
        routeOptions: RouteOptions,
        params: RouteOptionsAdapterParams,
    ): RouteOptions {
        return try {
            addContextAndReasonToRequest(params.signature, routeOptions)
        } catch (t: Throwable) {
            logE(LOG_TAG) {
                "Unhandled error: $t. Leaving original route options as is"
            }
            routeOptions
        }
    }

    private fun addContextAndReasonToRequest(
        signature: GetRouteSignature,
        routeOptions: RouteOptions,
    ): RouteOptions {
        val existingProperties = routeOptions.unrecognizedJsonProperties.orEmpty()
        val context = REROUTE_CONTEXT
        val reason = when (signature.reason) {
            GetRouteSignature.Reason.NEW_ROUTE -> return routeOptions
            GetRouteSignature.Reason.REROUTE_BY_DEVIATION -> REROUTE_DEVIATION_REASON
            GetRouteSignature.Reason.REROUTE_OTHER -> REROUTE_PARAMETERS_CHANGE_REASON
        }
        logD(LOG_TAG, "Adding $context $reason to reroute request")
        return routeOptions.toBuilder()
            .unrecognizedJsonProperties(
                existingProperties +
                    mapOf(CONTEXT_QUERY_PARAM to JsonPrimitive(context)) +
                    mapOf(REASON_QUERY_PARAM to JsonPrimitive(reason)),
            )
            .build()
    }
}
