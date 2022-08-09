package com.mapbox.navigation.dropin.binder.infopanel

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.Insets
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationViewContext
import com.mapbox.navigation.dropin.R
import com.mapbox.navigation.dropin.component.infopanel.InfoPanelComponent
import com.mapbox.navigation.ui.base.lifecycle.UIBinder
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * Base Binder class used for inflating and binding Info Panel layout.
 * Use [InfoPanelBinder.defaultBinder] to access default implementation.
 */
@ExperimentalPreviewMapboxNavigationAPI
abstract class InfoPanelBinder : UIBinder {

    private var headerBinder: UIBinder? = null
    private var contentBinder: UIBinder? = null
    internal var context: NavigationViewContext? = null

    /**
     * Create layout that will host both header and content layouts.
     *
     * @param layoutInflater The LayoutInflater service.
     * @param root Parent view that provides a set of LayoutParams values for root of the returned hierarchy.
     *
     * @return ViewGroup that can host both header and content layouts. Returned view must not be attached to [root] view.
     */
    abstract fun onCreateLayout(layoutInflater: LayoutInflater, root: ViewGroup): ViewGroup

    /**
     * Get layout that can be passed to header UIBinder.
     *
     * @param layout ViewGroup returned by [onCreateLayout]
     *
     * @return ViewGroup that will be passed to the header UIBinder to install header view or
     *  `null` if header view is not supported by parent [layout].
     */
    abstract fun getHeaderLayout(layout: ViewGroup): ViewGroup?

    /**
     * Get layout that can be passed to content UIBinder.
     *
     * @param layout ViewGroup returned by [onCreateLayout]
     *
     * @return ViewGroup that will be passed to the content UIBinder to install content view or
     *  `null` if content view is not supported by parent [layout].
     */
    abstract fun getContentLayout(layout: ViewGroup): ViewGroup?

    /**
     * Called when the Info Panel layout should apply system bar insets.
     *
     * This method should be overridden by a subclass to apply a policy different from the default behavior.
     * The default behavior applies insets with a value of [Insets.NONE].
     *
     * @param layout ViewGroup returned by [onCreateLayout]
     * @param insets system bars insets
     */
    open fun applySystemBarsInsets(layout: ViewGroup, insets: Insets) = Unit

    internal fun setBinders(headerBinder: UIBinder?, contentBinder: UIBinder?) {
        this.headerBinder = headerBinder
        this.contentBinder = contentBinder
    }

    internal fun setNavigationViewContext(context: NavigationViewContext) {
        this.context = context
    }

    /**
     * Triggered when this view binder instance is attached. The [viewGroup] returns a
     * [MapboxNavigationObserver] which gives this view a simple lifecycle.
     */
    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val layout = onCreateLayout(
            viewGroup.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE) as LayoutInflater,
            viewGroup
        )
        viewGroup.removeAllViews()
        viewGroup.addView(layout)

        val binders = mutableListOf<MapboxNavigationObserver>()
        ifNonNull(headerBinder, getHeaderLayout(layout)) { binder, headerLayout ->
            binders.add(binder.bind(headerLayout))
        }
        ifNonNull(contentBinder, getContentLayout(layout)) { binder, contentLayout ->
            binders.add(binder.bind(contentLayout))
        }

        context?.apply {
            applySystemBarsInsets(layout, systemBarsInsets.value)
        }

        return navigationListOf(*binders.toTypedArray())
    }

    companion object {
        /**
         * Default Info Panel Binder.
         */
        fun defaultBinder(): InfoPanelBinder = MapboxInfoPanelBinder()
    }
}

@ExperimentalPreviewMapboxNavigationAPI
internal class MapboxInfoPanelBinder : InfoPanelBinder() {

    override fun onCreateLayout(layoutInflater: LayoutInflater, root: ViewGroup): ViewGroup {
        return layoutInflater
            .inflate(R.layout.mapbox_info_panel_layout, root, false) as ViewGroup
    }

    override fun getHeaderLayout(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.infoPanelHeader)

    override fun getContentLayout(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.infoPanelContent)

    override fun applySystemBarsInsets(layout: ViewGroup, insets: Insets) {
        layout.setPadding(
            layout.paddingLeft,
            layout.paddingTop,
            layout.paddingRight,
            insets.bottom
        )
    }

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val observer = super.bind(viewGroup)
        return context?.let { context ->
            val layout = viewGroup.findViewById<ViewGroup>(R.id.infoPanelContainer)
            navigationListOf(
                InfoPanelComponent(layout, context),
                observer
            )
        } ?: observer
    }
}
