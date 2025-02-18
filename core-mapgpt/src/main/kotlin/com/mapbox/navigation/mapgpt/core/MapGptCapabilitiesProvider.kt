package com.mapbox.navigation.mapgpt.core

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

/**
 * Combines the capabilities of multiple [MapGptCapabilities] services into a single
 * [MapGptCapabilities] stream set.
 */
class MapGptCapabilitiesProvider(
    scope: CoroutineScope,
    capabilitiesServices: Set<MapGptCapabilities>,
) : MapGptCapabilities {

    private val customCapabilities = MutableStateFlow<Set<MapGptCapability>>(emptySet())

    override val capabilities: StateFlow<Set<MapGptCapability>> =
        combine(capabilitiesServices.map { it.capabilities }) { capabilitiesSets ->
            capabilitiesSets.fold(emptySet<MapGptCapability>()) { acc, set ->
                acc.union(set)
            }
        }.combine(customCapabilities) { provided, custom ->
            provided + custom
        }.stateIn(
            scope = scope,
            started = SharingStarted.Eagerly,
            initialValue = emptySet(),
        )

    private val disabledCapabilities = MutableStateFlow<Set<MapGptCapability>>(emptySet())

    fun capabilityIds(): Set<String> {
        val disabled = disabledCapabilities.value
        return capabilities.value
            .filter { !disabled.contains(it) }
            .map { it.capabilityId }.toSet()
    }
}
