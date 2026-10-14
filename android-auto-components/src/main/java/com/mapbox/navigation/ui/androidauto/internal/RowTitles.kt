package com.mapbox.navigation.ui.androidauto.internal

/**
 * The first of [candidates] that isn't blank, to use as a row title. A row can't have an empty
 * title, and place and search names can be empty. Null when every candidate is blank.
 */
internal fun rowTitle(vararg candidates: String?): String? =
    candidates.firstOrNull { !it.isNullOrBlank() }
