package com.mapbox.navigation.instrumentation_tests.utils

import android.content.Context
import com.mapbox.mapboxsdk.Mapbox

object Utils {

    /**
     * Returns the Mapbox access token set in the app resources.
     *
     * It will first search for a token in the Mapbox object. If not found it
     * will then attempt to load the access token from the
     * `res/values/dev.xml` development file.
     *
     * @param context The [Context] of the [android.app.Activity] or [android.app.Fragment].
     * @return The Mapbox access token or null if not found.
     */
    fun getMapboxAccessToken(context: Context): String? {
        return try {
            // Read out AndroidManifest
            val token = Mapbox.getAccessToken()
            require(!(token == null || token.isEmpty()))
            token
        } catch (exception: Exception) {
            // Use fallback on string resource, used for development
            val tokenResId = context.resources
                .getIdentifier("mapbox_access_token", "string", context.packageName)
            if (tokenResId != 0) context.getString(tokenResId) else null
        }
    }
}
