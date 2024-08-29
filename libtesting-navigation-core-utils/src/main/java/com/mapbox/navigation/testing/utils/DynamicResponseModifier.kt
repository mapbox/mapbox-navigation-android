package com.mapbox.navigation.testing.utils

import com.mapbox.api.directionsrefresh.v1.models.DirectionsRefreshResponse

class DynamicResponseModifier : (String) -> String {

    var numberOfInvocations = 0

    override fun invoke(p1: String): String {
        numberOfInvocations++
        val originalResponse = DirectionsRefreshResponse.fromJson(p1)
        val newRoute = originalResponse.route()!!
            .toBuilder()
            .legs(
                originalResponse.route()!!.legs()!!.map {
                    it
                        .toBuilder()
                        .annotation(
                            it.annotation()!!
                                .toBuilder()
                                .speed(
                                    it.annotation()!!.speed()!!.map {
                                        it + numberOfInvocations * 0.1
                                    }
                                )
                                .build()
                        )
                        .build()
                }
            )
            .build()
        return DirectionsRefreshResponse.builder()
            .route(newRoute)
            .code(originalResponse.code())
            .message(originalResponse.message())
            .build()
            .toJson()
    }
}
