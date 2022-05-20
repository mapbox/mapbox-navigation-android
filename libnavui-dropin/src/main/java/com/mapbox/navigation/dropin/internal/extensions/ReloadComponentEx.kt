@file:JvmName("ReloadComponentEx")

package com.mapbox.navigation.dropin.internal.extensions

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

/**
 * Return UIComponent that gets re-created using [factory] when [flow] changes.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal fun <T> reloadOnChange(
    flow: Flow<T>,
    factory: (T) -> UIComponent?
): UIComponent =
    ReloadingComponent(flow, factory)

/**
 * Return UIComponent that gets re-created using [factory] when either [flow1] or [flow2] changes.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal fun <T1, T2> reloadOnChange(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    factory: (T1, T2) -> UIComponent?
): UIComponent =
    ReloadingComponent(combine(flow1, flow2) { v1, v2 -> v1 to v2 }) {
        factory(it.first, it.second)
    }

/**
 * Return UIComponent that gets re-created using [factory] when either [flow1] or [flow2] or
 * [flow3] changes.
 */
@ExperimentalPreviewMapboxNavigationAPI
internal fun <T1, T2, T3> reloadOnChange(
    flow1: Flow<T1>,
    flow2: Flow<T2>,
    flow3: Flow<T3>,
    factory: (T1, T2, T3) -> UIComponent?
): UIComponent =
    ReloadingComponent(combine(flow1, flow2, flow3) { v1, v2, v3 -> Triple(v1, v2, v3) }) {
        factory(it.first, it.second, it.third)
    }
