package com.mapbox.navigation.qa_test_app.view.customnavview

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.Insets
import androidx.core.view.updateLayoutParams
import androidx.core.view.updatePadding
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.internal.extensions.navigationListOf
import com.mapbox.navigation.core.lifecycle.MapboxNavigationObserver
import com.mapbox.navigation.dropin.NavigationView
import com.mapbox.navigation.dropin.ViewStyleCustomization.Companion.defaultInfoPanelPeekHeight
import com.mapbox.navigation.dropin.infopanel.InfoPanelBinder
import com.mapbox.navigation.dropin.internal.extensions.updateMargins
import com.mapbox.navigation.dropin.navigationview.NavigationViewListener
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.ui.base.lifecycle.UIComponent
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CustomInfoPanelBinderWithFixedHeight(
    private val navigationView: NavigationView
) : InfoPanelBinder() {
    private var myLayout: ViewGroup? = null

    override fun onCreateLayout(
        layoutInflater: LayoutInflater,
        root: ViewGroup
    ): ViewGroup {
        val layout = layoutInflater.inflate(R.layout.layout_info_panel, root, false) as ViewGroup
        myLayout = layout
        return layout
    }

    override fun getHeaderLayout(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.infoPanelHeader)

    override fun getContentLayout(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.infoPanelContent)

    override fun applySystemBarsInsets(layout: ViewGroup, insets: Insets) {
        this.insets = insets
        layout.updateMargins(left = 10.dp + insets.left, right = 10.dp + insets.right)
    }

    override fun bind(viewGroup: ViewGroup): MapboxNavigationObserver {
        val observer = super.bind(viewGroup)
        val layout = viewGroup.findViewById<ViewGroup>(R.id.infoPanelContainer)
        layout.updateLayoutParams {
            height = navigationView.height / 2
        }
        return navigationListOf(
            observer,
            CustomInfoPanelComponentForFixedHeight(layout, insets, slideOffsetFlow)
        )
    }

    private var insets: Insets = Insets.NONE
    private val slideOffsetFlow = MutableStateFlow(-1f)

    private val slideOffsetObserver = object : NavigationViewListener() {
        override fun onInfoPanelSlide(slideOffset: Float) {
            this@CustomInfoPanelBinderWithFixedHeight.slideOffsetFlow.value = slideOffset
        }
    }

    fun setEnabled(enabled: Boolean) {
        if (enabled) {
            navigationView.addListener(slideOffsetObserver)
            navigationView.customizeViewBinders {
                infoPanelBinder = this@CustomInfoPanelBinderWithFixedHeight
            }
            navigationView.customizeViewStyles {
                val context = navigationView.context
                infoPanelPeekHeight = defaultInfoPanelPeekHeight(context) + insets.bottom
            }
        } else {
            navigationView.removeListener(slideOffsetObserver)
        }
    }
}

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
private class CustomInfoPanelComponentForFixedHeight(
    private val layout: ViewGroup,
    private val insets: Insets,
    private val slideOffsetFlow: StateFlow<Float>
) : UIComponent() {

    override fun onAttached(mapboxNavigation: MapboxNavigation) {
        super.onAttached(mapboxNavigation)
        slideOffsetFlow.observe { slideOffset ->
            val threshold = 0.7
            if (threshold < slideOffset) {
                val f = 1.0f - (1.0f - slideOffset) / (1.0f - threshold)
                val margins = (10.dp - (10.dp * f)).toInt()
                layout.updatePadding(top = 0, bottom = insets.bottom)
                layout.updateMargins(left = margins + insets.left, right = margins + insets.right)
                layout.background.updateCornerRadius(20.dp - (20.dp * f))
            } else {
                layout.updatePadding(top = 0, bottom = insets.bottom)
                layout.updateMargins(left = 10.dp + insets.left, right = 10.dp + insets.right)
                layout.background.updateCornerRadius(20.dp)
            }
        }
    }

    private fun Drawable.updateCornerRadius(radius: Number) {
        (this as? GradientDrawable)?.cornerRadius = radius.toFloat()
    }
}
