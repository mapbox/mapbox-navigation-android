package com.mapbox.navigation.instrumentation_tests.utils

import android.content.Context
import androidx.annotation.IntegerRes
import okio.Buffer

fun readRawFileText(context: Context, @IntegerRes res: Int): String =
    context.resources.openRawResource(res).bufferedReader().use { it.readText() }

fun bufferFromRawFile(context: Context, @IntegerRes res: Int): Buffer =
    Buffer().readFrom(context.resources.openRawResource(res))
