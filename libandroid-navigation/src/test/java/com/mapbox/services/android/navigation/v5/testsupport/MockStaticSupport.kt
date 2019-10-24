package com.mapbox.services.android.navigation.v5.testsupport

import io.mockk.mockkStatic

internal inline fun mockkStaticSupport(vararg extension: Extensions) {
    mockkStatic(*extension.map { it.path }.toTypedArray())
}

internal enum class Extensions(val path: String) {
    ContextEx("com.mapbox.navigation.utils.extensions.ContextEx"),
    LocaleEx("com.mapbox.navigation.utils.extensions.LocaleEx"),
    SpanEx("com.mapbox.navigation.utils.extensions.SpanEx");
}
