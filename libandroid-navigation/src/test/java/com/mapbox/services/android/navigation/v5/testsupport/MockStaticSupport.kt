package com.mapbox.services.android.navigation.v5.testsupport

import io.mockk.mockkStatic

internal inline fun mockStaticSupport(vararg extension: Extensions) {
    mockkStatic(*extension.map { it.path }.toTypedArray())
}

internal enum class Extensions(val path: String) {
    ContextEx("com.mapbox.services.android.navigation.v5.utils.extensions.ContextEx"),
    LocaleEx("com.mapbox.services.android.navigation.v5.utils.extensions.LocaleEx"),
    SpanEx("com.mapbox.services.android.navigation.v5.utils.extensions.SpanEx");
}
