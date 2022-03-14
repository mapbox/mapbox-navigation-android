package com.mapbox.navigation.navigator.internal

import com.mapbox.navigation.utils.internal.logE
import org.json.JSONException
import org.json.JSONObject

// TODO Remove after NN enable internal reroute by default
internal fun String.customConfigEnableNativeRerouteInterface(): String {
    val rootJsonObj = if (this.isNotBlank()) {
        try {
            JSONObject(this)
        } catch (e: JSONException) {
            logE("custom config json is not valid: $e")
            JSONObject()
        }
    } else {
        JSONObject()
    }

    val featuresJsonObj = if (rootJsonObj.has("features")) {
        rootJsonObj.getJSONObject("features")
    } else {
        JSONObject().also {
            rootJsonObj.put("features", it)
        }
    }

    featuresJsonObj.put("useInternalReroute", true)

    return rootJsonObj.toString()
}
