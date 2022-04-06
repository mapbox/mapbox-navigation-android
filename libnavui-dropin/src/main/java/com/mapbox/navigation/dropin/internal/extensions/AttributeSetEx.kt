@file:JvmName("AttributeSetEx")

package com.mapbox.navigation.dropin.internal.extensions

import android.content.Context
import android.util.AttributeSet
import androidx.core.content.res.use
import com.mapbox.navigation.dropin.R

fun AttributeSet?.navigationViewAccessToken(context: Context): String {
    val accessToken = context.obtainStyledAttributes(
        this,
        R.styleable.NavigationView,
        0,
        0
    ).use { it.getString(R.styleable.NavigationView_accessToken) }
    checkNotNull(accessToken) {
        "Provide access token directly in the constructor or via 'accessToken' layout parameter"
    }
    return accessToken
}
