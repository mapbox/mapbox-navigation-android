package com.mapbox.navigation.testing.ui.utils

import android.content.Context

fun getMapboxAccessTokenFromResources(context: Context) = context.getString(
    context.resources.getIdentifier("mapbox_access_token", "string", context.packageName)
)
