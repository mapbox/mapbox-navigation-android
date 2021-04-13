package com.mapbox.navigation.qa_test_app.utils

import android.content.Context

object Utils {

    fun getMapboxAccessToken(context: Context): String {
        return context.getString(
            context.resources.getIdentifier(
                "mapbox_access_token",
                "string",
                context.packageName
            )
        )
    }
}
