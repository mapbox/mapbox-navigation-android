package com.mapbox.navigation.testing.utils

import android.content.Context
import androidx.annotation.RawRes
import okio.Buffer
import java.io.InputStream

fun readRawFileText(context: Context, @RawRes res: Int): String =
    context.resources.openRawResource(res).bufferedReader().use { it.readText() }

fun openRawResource(context: Context, @RawRes res: Int): InputStream =
    context.resources.openRawResource(res)

fun bufferFromRawFile(context: Context, @RawRes res: Int): Buffer =
    Buffer().readFrom(context.resources.openRawResource(res))
