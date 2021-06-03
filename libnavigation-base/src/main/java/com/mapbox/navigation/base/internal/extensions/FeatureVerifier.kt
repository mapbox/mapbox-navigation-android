package com.mapbox.navigation.base.internal.extensions

import com.mapbox.api.directions.v5.DirectionsCriteria
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.base.common.logger.Logger
import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag

private val TAG = Tag("MbxFeatureVerifier")

@Throws(RuntimeException::class)
fun List<DirectionsRoute>.verifyCompatibility(
    logger: Logger,
    verifierConf: RouteFeatureVerifiers.() -> Unit
) {
    this.forEach {
        it.verifyCompatibility(logger, verifierConf)
    }
}

@Throws(RuntimeException::class)
fun DirectionsRoute.verifyCompatibility(
    logger: Logger,
    verifierConf: RouteFeatureVerifiers.() -> Unit
) {
    val errorMessageBuilder = StringBuilder()
    errorMessageBuilder.append(
        "Provided route is not compatible with the Navigation SDK."
    )
    val verifiers = RouteFeatureVerifiers().apply(verifierConf)
    var exception = false
    var error = false
    var warning = false
    verifiers.getAllVerifiers().filter { it.enabled }.forEach { verifier ->
        verifier.isIncompatible.forEach {
            val result = it(this)
            if (result != null) {
                when (verifier.severity) {
                    FeatureVerifier.Severity.Exception -> exception = true
                    FeatureVerifier.Severity.Error -> error = true
                    FeatureVerifier.Severity.Warning -> warning = true
                }
                errorMessageBuilder.append("\n")
                errorMessageBuilder.append(result)
            }
        }
    }
    if (exception) {
        throw RuntimeException(errorMessageBuilder.toString())
    }
    if (error) {
        logger.e(
            TAG,
            Message(errorMessageBuilder.toString())
        )
    }
    if (warning) {
        logger.w(
            TAG,
            Message(errorMessageBuilder.toString())
        )
    }
}

class FeatureVerifier<T> internal constructor(
    internal val severity: Severity,
    internal val isIncompatible: List<(T) -> String?>
) {
    var enabled: Boolean = false

    internal enum class Severity {
        Exception,
        Error,
        Warning
    }
}

class RouteFeatureVerifiers internal constructor() {
    val baseFeatures = FeatureVerifier<DirectionsRoute>(
        severity = FeatureVerifier.Severity.Exception,
        isIncompatible = listOf(
            { route ->
                if (route.geometry().isNullOrBlank()) {
                    "The route is missing geometry."
                } else {
                    null
                }
            },
            { route ->
                if (route.legs().isNullOrEmpty()) {
                    "The route is missing legs."
                } else {
                    null
                }
            },
            { route ->
                if (route.legs()!!.any { it.steps().isNullOrEmpty() }) {
                    "The route is missing steps."
                } else {
                    null
                }
            },
            { route ->
                if (route.routeOptions()!!.steps() != true) {
                    "The route options require RouteOptions#steps."
                } else {
                    null
                }
            },
            { route ->
                if (route.routeOptions()!!.geometries() == null) {
                    "The route options require RouteOptions#geometries."
                } else {
                    null
                }
            },
            { route ->
                if (route.routeOptions()!!.overview() != DirectionsCriteria.OVERVIEW_FULL) {
                    "The route options require RouteOptions#overview == DirectionsCriteria.OVERVIEW_FULL."
                } else {
                    null
                }
            }
        )
    )

    val progressAnnotationsFeatures = FeatureVerifier<DirectionsRoute>(
        severity = FeatureVerifier.Severity.Warning,
        isIncompatible = listOf(
            { route ->
                if (route.legs()!!.any { it.annotation()!!.duration() == null }) {
                    "The route is missing duration."
                } else {
                    null
                }
            }
        )
    )

    internal fun getAllVerifiers(): List<FeatureVerifier<DirectionsRoute>> = listOf(
        baseFeatures
    )
}
