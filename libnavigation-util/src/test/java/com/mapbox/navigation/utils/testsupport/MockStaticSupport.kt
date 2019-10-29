package com.mapbox.navigation.utils.testsupport

import io.mockk.mockkStatic

inline fun mockkStaticSupport(vararg extension: Extensions) {
    mockkStatic(*extension.map { it.path }.toTypedArray())
}

enum class Extensions(val path: String) {
    ContextEx("com.mapbox.navigation.utils.extensions.ContextEx"),
    LocaleEx("com.mapbox.navigation.utils.extensions.LocaleEx"),
    SpanEx("com.mapbox.navigation.utils.extensions.SpanEx");
}
