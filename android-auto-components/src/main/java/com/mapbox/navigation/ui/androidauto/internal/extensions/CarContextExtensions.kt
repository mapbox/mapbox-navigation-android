@file:JvmName("CarContextEx")

package com.mapbox.navigation.ui.androidauto.internal.extensions

import androidx.car.app.CarContext
import androidx.car.app.constraints.ConstraintManager
import com.mapbox.navigation.ui.androidauto.internal.logAndroidAutoFailure

/**
 * The maximum number of items the host shows in a list of the given content type, such as
 * [ConstraintManager.CONTENT_LIMIT_TYPE_PLACE_LIST]. Items beyond the limit are not shown, so
 * map content for them, such as markers, should not be shown either.
 *
 * This is a call to the host. If it fails, for example while the host unbinds, nothing is
 * trimmed and the host applies its own limit.
 */
internal fun CarContext.contentLimit(contentLimitType: Int): Int = try {
    getCarService(ConstraintManager::class.java).getContentLimit(contentLimitType)
} catch (e: RuntimeException) {
    logAndroidAutoFailure("getContentLimit($contentLimitType) failed", e)
    Int.MAX_VALUE
}
