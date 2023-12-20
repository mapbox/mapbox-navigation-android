package com.mapbox.navigation.utils.internal.android

import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable

fun Bitmap.toDrawable(resources: Resources) = BitmapDrawable(resources, this)
