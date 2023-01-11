package com.mapbox.navigation.ui.shield.internal.loader

import com.mapbox.bindgen.Expected

internal fun interface ResourceLoader<Argument, Resource> {
    suspend fun load(argument: Argument): Expected<String, Resource>
}
