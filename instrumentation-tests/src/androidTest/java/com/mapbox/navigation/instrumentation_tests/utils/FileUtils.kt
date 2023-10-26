package com.mapbox.navigation.instrumentation_tests.utils

import android.content.Context
import androidx.annotation.IntegerRes
import okio.Buffer
import java.io.InputStream

fun readRawFileText(context: Context, @IntegerRes res: Int): String =
    context.resources.openRawResource(res).bufferedReader().use { it.readText() }

fun openRawResource(context: Context, @IntegerRes res: Int): InputStream =
    context.resources.openRawResource(res)

fun bufferFromRawFile(context: Context, @IntegerRes res: Int): Buffer =
    Buffer().readFrom(context.resources.openRawResource(res))
