package com.mapbox.navigation.dropin.actionbutton

import android.content.res.Resources
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.Px
import androidx.annotation.UiThread
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.EmptyBinder
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.ViewBinderCustomization
import com.mapbox.navigation.dropin.internal.extensions.audioGuidanceButtonComponent
import com.mapbox.navigation.dropin.internal.extensions.cameraModeButtonComponent
import com.mapbox.navigation.dropin.internal.extensions.compassButtonComponent
import com.mapbox.navigation.dropin.internal.extensions.recenterButtonComponent
import com.mapbox.navigation.dropin.internal.extensions.updateMargins
import com.mapbox.navigation.dropin.navigationview.NavigationViewContext
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

/**
 * Base Binder class used for inflating and binding Action Buttons layout.
 * Use [ActionButtonsBinder.defaultBinder] to access default implementation.
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class ActionButtonsBinder : UIBinder {

    internal var context: NavigationViewContext? = null
    internal var customButtons: List<ActionButtonDescription> = emptyList()

    /**
     * Create layout that will host action buttons containers.
     *
     * @param layoutInflater The LayoutInflater service.
     * @param root Parent view that provides a set of LayoutParams values for root of the returned hierarchy.
     *
     * @return ViewGroup that can host action button layout.
     *   Returned view must not be attached to the root view.
     */
    @UiThread
    abstract fun onCreateLayout(layoutInflater: LayoutInflater, root: ViewGroup): ViewGroup

    /**
     * Get layout that can used to host custom Action Buttons with [ActionButtonDescription.Position.START].
     *
     * @param layout ViewGroup returned by [onCreateLayout]
     *
     * @return ViewGroup that will be used to install buttons or `null` if custom buttons
     *   are supported by parent [layout].
     */
    @UiThread
    protected abstract fun getCustomButtonsStartContainer(layout: ViewGroup): ViewGroup?

    /**
     * Get layout that can be passed to the Compass Button UIBinder
     * @see [ViewBinderCustomization.actionCompassButtonBinder]
     *
     * @param layout ViewGroup returned by [onCreateLayout]
     *
     * @return ViewGroup that will be passed to the UIBinder to install the compass button or
     *  `null` if the button should not be installed.
     */
    @UiThread
    protected abstract fun getCompassButtonContainer(layout: ViewGroup): ViewGroup?

    /**
     * Get layout that can be passed to the Camera Mode Button UIBinder
     * @see [ViewBinderCustomization.actionCameraModeButtonBinder]
     *
     * @param layout ViewGroup returned by [onCreateLayout]
     *
     * @return ViewGroup that will be passed to the UIBinder to install the camera mode button or
     *  `null` if the button should not be installed.
     */
    @UiThread
    protected abstract fun getCameraModeButtonContainer(layout: ViewGroup): ViewGroup?

    /**
     * Get layout that can be passed to the Toggle Audio Guidance Button UIBinder
     * @see [ViewBinderCustomization.actionToggleAudioButtonBinder]
     *
     * @param layout ViewGroup returned by [onCreateLayout]
     *
     * @return ViewGroup that will be passed to the UIBinder to install the toggle audio guidance button or
     *  `null` if the button should not be installed.
     */
    @UiThread
    protected abstract fun getToggleAudioButtonContainer(layout: ViewGroup): ViewGroup?

    /**
     * Get layout that can be passed to the Recenter Camera Button UIBinder
     * @see [ViewBinderCustomization.actionRecenterButtonBinder]
     *
     * @param layout ViewGroup returned by [onCreateLayout]
     *
     * @return ViewGroup that will be passed to the UIBinder to install the recenter camera button or
     *  `null` if the button should not be installed.
     */
    @UiThread
    protected abstract fun getRecenterButtonContainer(layout: ViewGroup): ViewGroup?

    /**
     * Get layout that can used to host custom Action Buttons with [ActionButtonDescription.Position.END].
     *
     * @param layout ViewGroup returned by [onCreateLayout]
     *
     * @return ViewGroup that will be used to install buttons or `null` if custom buttons
     *   are supported by parent [layout].
     */
    @UiThread
    protected abstract fun getCustomButtonsEndContainer(layout: ViewGroup): ViewGroup?

    /**
     * Vertical spacing between custom buttons.
     */
    @Px
    @UiThread
    protected open fun verticalSpacing(resources: Resources): Int =
        resources.getDimensionPixelSize(R.dimen.mapbox_actionList_spacing)

    /**
     * Triggered when this view binder instance is attached. The [viewGroup] returns a
     * [MapboxNavigationObserver] which gives this view a simple lifecycle.
     */
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val navigationContext = context ?: return EmptyBinder().bind(viewGroup)

        val layout = onCreateLayout(LayoutInflater.from(viewGroup.context), viewGroup)
        viewGroup.removeAllViews()
        viewGroup.addView(layout)

        getCustomButtonsStartContainer(layout)?.also { installStartButtons(it) }
        getCustomButtonsEndContainer(layout)?.also { installEndButtons(it) }

        return navigationContext.run {
            val components = mutableListOf<MapboxNavigationObserver>()

            getCompassButtonContainer(layout)?.also {
                components.add(compassButtonComponent(it))
            }
            getCameraModeButtonContainer(layout)?.also {
                components.add(cameraModeButtonComponent(it))
            }
            getToggleAudioButtonContainer(layout)?.also {
                components.add(audioGuidanceButtonComponent(it))
            }
            getRecenterButtonContainer(layout)?.also {
                components.add(recenterButtonComponent(it))
            }

            navigationListOf(*components.toTypedArray())
        }
    }

    private fun installStartButtons(layout: ViewGroup) {
        val spacing = verticalSpacing(layout.resources)
        customButtons
            .filter { it.position == ActionButtonDescription.Position.START }
            .asReversed()
            .forEach {
                (it.view.parent as? ViewGroup)?.apply { removeView(it.view) }
                layout.addView(it.view, 0)
                it.view.updateMargins(top = spacing, bottom = spacing)
            }
    }

    private fun installEndButtons(layout: ViewGroup) {
        val spacing = verticalSpacing(layout.resources)
        customButtons
            .filter { it.position == ActionButtonDescription.Position.END }
            .forEach {
                (it.view.parent as? ViewGroup)?.apply { removeView(it.view) }
                layout.addView(it.view)
                it.view.updateMargins(top = spacing, bottom = spacing)
            }
    }

    companion object {
        /**
         * Default Action Buttons Binder.
         */
        fun defaultBinder(): ActionButtonsBinder = MapboxActionButtonsBinder()
    }
}
