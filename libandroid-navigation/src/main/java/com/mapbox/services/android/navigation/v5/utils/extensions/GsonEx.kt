@file:JvmName("ContextEx")

package com.mapbox.services.android.navigation.v5.utils.extensions

import com.google.gson.Gson

inline fun <reified T> Gson.fromJson(string: String): T =
    this.fromJson(string, T::class.java)
