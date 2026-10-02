package com.mapbox.navigation.ui.androidauto.screenmanager

import androidx.annotation.StringDef
import androidx.car.app.Screen
import androidx.car.app.ScreenManager
import androidx.car.app.Session
import androidx.car.app.model.Template

object MapboxScreenOperation {
    /**
     * Triggered when [MapboxScreenManager.createScreen] is used.
     *
     * The [Session.onCreateScreen] requires a [Screen] to be returned directly which is typical
     * reason for this event.
     */
    const val CREATED = "CREATED"

    /**
     * Triggered when [MapboxScreenManager.replaceTop] is called.
     *
     * The [Screen] will replace the top of the [ScreenManager] backstack. If the top is already
     * set to the screen, this operation will not change the screen or the backstack.
     */
    const val REPLACE_TOP = "REPLACE_TOP"

    /**
     * Triggered when [MapboxScreenManager.push] is called.
     *
     * The [Screen] will be pushed onto the [ScreenManager] backstack. Be aware that there must be
     * less than 5 [Template] instances at a time.
     *
     * https://developer.android.com/training/cars/apps#template-restrictions
     */
    const val PUSH = "PUSH"

    /**
     * Triggered when [MapboxScreenManager.goBack] is called and the call successfully changed
     * screens to a previous screen on the back-stack.
     */
    const val GO_BACK = "GO_BACK"

    /**
     * Retention policy for the OpenLRSideOfRoad
     */
    @Retention(AnnotationRetention.BINARY)
    @StringDef(
        CREATED,
        REPLACE_TOP,
        PUSH,
        GO_BACK,
    )
    annotation class Type
}
