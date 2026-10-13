package com.mapbox.navigation.ui.androidauto.internal

import androidx.car.app.CarContext
import androidx.car.app.model.Action
import androidx.car.app.model.GridTemplate
import androidx.car.app.model.Header
import androidx.car.app.model.ListTemplate
import androidx.car.app.model.MessageTemplate
import androidx.car.app.versioning.CarAppApiLevels

/*
 * Template headers for every Car API level the SDK supports.
 *
 * Car App Library 1.7.0 deprecates the template title and header action setters in favor of
 * setHeader(Header). setHeader is available from Car API 7 on list, grid and message templates,
 * and from Car API 5 on the place list and route preview templates:
 * - list, grid and message templates fill the legacy fields from setHeader, so they need the
 *   legacy setters only below Car API 7, where setHeader is unavailable;
 * - place list and route preview templates keep the header and the legacy fields apart, and are
 *   deprecated as a whole, so the legacy fields are always set, for hosts that still read them.
 *
 * An empty title is treated as no title: a Header needs a non-empty title or a start action, and
 * the templates build their header from the legacy fields as well. When neither is given, no header
 * and no legacy fields are set.
 */

internal fun ListTemplate.Builder.setHeaderCompat(
    carContext: CarContext,
    title: CharSequence?,
    startAction: Action?,
): ListTemplate.Builder = setHeaderCompat(
    carContext,
    headerCarApiLevel = CarAppApiLevels.LEVEL_7,
    keepLegacyFields = false,
    title,
    startAction,
    setHeader = { setHeader(it) },
    setLegacyTitle = @Suppress("DEPRECATION") { setTitle(it) },
    setLegacyStartAction = @Suppress("DEPRECATION") { setHeaderAction(it) },
)

internal fun GridTemplate.Builder.setHeaderCompat(
    carContext: CarContext,
    title: CharSequence?,
    startAction: Action?,
): GridTemplate.Builder = setHeaderCompat(
    carContext,
    headerCarApiLevel = CarAppApiLevels.LEVEL_7,
    keepLegacyFields = false,
    title,
    startAction,
    setHeader = { setHeader(it) },
    setLegacyTitle = @Suppress("DEPRECATION") { setTitle(it) },
    setLegacyStartAction = @Suppress("DEPRECATION") { setHeaderAction(it) },
)

internal fun MessageTemplate.Builder.setHeaderCompat(
    carContext: CarContext,
    title: CharSequence?,
    startAction: Action?,
): MessageTemplate.Builder = setHeaderCompat(
    carContext,
    headerCarApiLevel = CarAppApiLevels.LEVEL_7,
    keepLegacyFields = false,
    title,
    startAction,
    setHeader = { setHeader(it) },
    setLegacyTitle = @Suppress("DEPRECATION") { setTitle(it) },
    setLegacyStartAction = @Suppress("DEPRECATION") { setHeaderAction(it) },
)

// PlaceListNavigationTemplate is deprecated with Car API 7, and is built only below it.
@Suppress("DEPRECATION")
private typealias PlaceListBuilder =
    androidx.car.app.navigation.model.PlaceListNavigationTemplate.Builder

// RoutePreviewNavigationTemplate is deprecated with Car API 7. The legacy ROUTE_PREVIEW screen
// still builds it on newer hosts.
@Suppress("DEPRECATION")
private typealias RoutePreviewBuilder =
    androidx.car.app.navigation.model.RoutePreviewNavigationTemplate.Builder

@Suppress("DEPRECATION")
internal fun PlaceListBuilder.setHeaderCompat(
    carContext: CarContext,
    title: CharSequence?,
    startAction: Action?,
): PlaceListBuilder = setHeaderCompat(
    carContext,
    headerCarApiLevel = CarAppApiLevels.LEVEL_5,
    keepLegacyFields = true,
    title,
    startAction,
    setHeader = { setHeader(it) },
    setLegacyTitle = { setTitle(it) },
    setLegacyStartAction = { setHeaderAction(it) },
)

@Suppress("DEPRECATION")
internal fun RoutePreviewBuilder.setHeaderCompat(
    carContext: CarContext,
    title: CharSequence?,
    startAction: Action?,
): RoutePreviewBuilder = setHeaderCompat(
    carContext,
    headerCarApiLevel = CarAppApiLevels.LEVEL_5,
    keepLegacyFields = true,
    title,
    startAction,
    setHeader = { setHeader(it) },
    setLegacyTitle = { setTitle(it) },
    setLegacyStartAction = { setHeaderAction(it) },
)

private inline fun <B> B.setHeaderCompat(
    carContext: CarContext,
    headerCarApiLevel: Int,
    keepLegacyFields: Boolean,
    title: CharSequence?,
    startAction: Action?,
    setHeader: B.(Header) -> Unit,
    setLegacyTitle: B.(CharSequence) -> Unit,
    setLegacyStartAction: B.(Action) -> Unit,
): B = apply {
    val nonEmptyTitle = title?.takeIf { it.isNotEmpty() }
    val usesHeader = carContext.carAppApiLevel >= headerCarApiLevel
    if (usesHeader) {
        buildHeaderOrNull(nonEmptyTitle, startAction)?.let { setHeader(it) }
    }
    if (keepLegacyFields || !usesHeader) {
        nonEmptyTitle?.let { setLegacyTitle(it) }
        startAction?.let { setLegacyStartAction(it) }
    }
}

// Header.Builder.build() throws without a title or a start action.
private fun buildHeaderOrNull(title: CharSequence?, startAction: Action?): Header? {
    if (title == null && startAction == null) return null
    return Header.Builder()
        .apply {
            title?.let(::setTitle)
            startAction?.let(::setStartHeaderAction)
        }
        .build()
}
