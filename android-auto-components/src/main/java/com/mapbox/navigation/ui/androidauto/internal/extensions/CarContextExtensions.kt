@file:JvmName("CarContextEx")

package com.mapbox.navigation.ui.androidauto.internal.extensions

import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager

/**
 * The maximum number of items the host shows in a list of the given content type, such as
 * [ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST]. Items beyond the limit are not shown, so
 * map content for them, such as markers, should not be shown either.
 */
internal fun CarContext.contentLimit(contentLimitType: Int): Int =
    getCarService(ConstraintManager::class.java).getContentLimit(contentLimitType)
