package com.mapbox.navigation.utils.internal

import androidx.annotation.RestrictTo
import com.google.gson.JsonElement
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

@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
fun JsonElement.asIntOrNull(): Int? = when {
    isJsonNull -> null
    isJsonPrimitive -> asJsonPrimitive.let { prim ->
        when {
            prim.isNumber -> prim.asInt
            prim.isString -> prim.asString.toIntOrNull()
            else -> null
        }
    }
    else -> null
}
