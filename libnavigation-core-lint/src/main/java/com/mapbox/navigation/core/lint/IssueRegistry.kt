package com.mapbox.navigation.core.lint

import com.android.tools.lint.client.api.Vendor

class IssueRegistry : com.android.tools.lint.client.api.IssueRegistry() {
    override val issues = listOf(RouteOptionsDetector.ISSUE)
    override val vendor: Vendor?
        get() = Vendor(
            vendorName = "mapbox"
        )
}
