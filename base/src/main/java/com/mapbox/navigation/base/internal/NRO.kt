package com.mapbox.navigation.base.internal

import androidx.annotation.RestrictTo
import com.google.gson.JsonArray
import com.mapbox.api.directions.v5.models.BannerInstructions
import com.mapbox.api.directions.v5.models.BannerInstructionsFBWrapper
import com.mapbox.api.directions.v5.models.DirectionsResponse
import com.mapbox.api.directions.v5.models.DirectionsResponseFBWrapper
import com.mapbox.api.directions.v5.models.LegAnnotation
import com.mapbox.api.directions.v5.models.LegAnnotationFBWrapper
import com.mapbox.api.directions.v5.models.utils.BaseFBWrapper

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun DirectionsResponse.isNativeRoute(): Boolean {
    return this is DirectionsResponseFBWrapper
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun BannerInstructions.isNative(): Boolean {
    return this is BannerInstructionsFBWrapper
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
interface StateOfCharge {
    val size: Int
    fun get(index: Int): Double
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun LegAnnotation.stateOfCharge(): StateOfCharge? {
    return if (this is LegAnnotationFBWrapper) {
        this.stateOfCharge
    } else {
        val soc = this.getUnrecognizedProperty("state_of_charge") ?: return null
        if (soc.isJsonArray) {
            UnrecognizedStateOfCharge(soc.asJsonArray)
        } else {
            null
        }
    }
}

private class UnrecognizedStateOfCharge(
    private val jsonArray: JsonArray,
) : StateOfCharge {
    override val size: Int
        get() = jsonArray.size()

    override fun get(index: Int): Double {
        return jsonArray[index].asDouble
    }
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun NotSupportedForNativeRouteObject(featureName: String): Nothing {
    throw NotSupportedForNativeRouteObjectException(featureName)
}

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
class NotSupportedForNativeRouteObjectException(val featureName: String) : Throwable(
    "$featureName is not supported for native route objects.",
)

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun Any.internalUnrecognizedMap(): Map<String, Any?>? {
    return if (this is BaseFBWrapper) {
        this.unrecognizedMap()
    } else {
        throw IllegalArgumentException("Object is not a BaseFBWrapper")
    }
}
