package com.mapbox.navigation.qa_test_app.view.util

import android.content.Context

object Utils {
    /**
     * Returns the Mapbox access token set in the app resources.
     *
     * @param context The [Context] of the [android.app.Activity] or [android.app.Fragment].
     * @return The Mapbox access token or null if not found.
     */
    fun getMapboxAccessToken(context: Context): String {
        val tokenResId = context.resources
            .getIdentifier("mapbox_access_token", "string", context.packageName)
        return if (tokenResId != 0) context.getString(tokenResId) else ""
    }
}
