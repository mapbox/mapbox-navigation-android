package com.mapbox.navigation.utils.internal

import androidx.annotation.RestrictTo
import org.json.JSONObject

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun JSONObject.getOrPutJsonObject(objectName: String): JSONObject {
    return if (has(objectName)) {
        getJSONObject(objectName)
    } else {
        JSONObject().also {
            put(objectName, it)
        }
    }
}
