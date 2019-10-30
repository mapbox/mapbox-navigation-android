@file:JvmName("GsonEx")

package com.mapbox.services.android.navigation.v5.internal.utils.extensions

import com.google.gson.Gson

internal inline fun <reified T> Gson.fromJson(string: String): T =
    this.fromJson(string, T::class.java)
