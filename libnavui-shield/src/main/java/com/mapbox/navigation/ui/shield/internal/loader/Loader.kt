package com.mapbox.navigation.ui.shield.internal.loader

import com.mapbox.bindgen.Expected

internal fun interface Loader<Input, Output> {
    suspend fun load(input: Input): Expected<Error, Output>
}
