package com.mapbox.navigation.qa_test_app.view.customnavview

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.graphics.Insets
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.binder.infopanel.InfoPanelBinder
import com.mapbox.navigation.qa_test_app.R

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class CustomInfoPanelBinder : InfoPanelBinder() {
    override fun onCreateLayout(
        layoutInflater: LayoutInflater,
        root: ViewGroup
    ): ViewGroup {
        return layoutInflater.inflate(R.layout.layout_info_panel, root, false) as ViewGroup
    }

    override fun getHeaderLayout(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.infoPanelHeader)

    override fun getContentLayout(layout: ViewGroup): ViewGroup? =
        layout.findViewById(R.id.infoPanelContent)

    override fun applySystemBarsInsets(layout: ViewGroup, insets: Insets) {
        layout.layoutParams = (layout.layoutParams as ViewGroup.MarginLayoutParams).apply {
            setMargins(leftMargin, topMargin, rightMargin, 10.dp + insets.bottom)
        }
    }
}
