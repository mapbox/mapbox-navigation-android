@file:JvmName("PrimitivesEx")

package com.mapbox.navigation.utils.internal

fun Double?.toFloatOrNull(): Float? =
    this?.toFloat()
