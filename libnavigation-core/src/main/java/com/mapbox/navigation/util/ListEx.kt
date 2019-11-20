package com.mapbox.navigation.util

import com.google.gson.reflect.TypeToken

internal inline fun <reified T> rawType() = object : TypeToken<T>() {}.rawType
