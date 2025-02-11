package com.mapbox.navigation.navigator.internal.util

import android.content.Context
import androidx.annotation.IntegerRes

fun readRawFileText(context: Context, @IntegerRes res: Int): String =
    context.resources.openRawResource(res).bufferedReader().use { it.readText() }
