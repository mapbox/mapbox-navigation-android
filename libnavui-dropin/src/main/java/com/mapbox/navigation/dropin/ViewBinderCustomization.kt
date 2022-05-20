package com.mapbox.navigation.dropin

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.ui.base.lifecycle.UIBinder

/**
 * A class that allows you to define [UIBinder] for various different views used by the
 * [NavigationView]. If not specified, [NavigationView] uses the default [UIBinder] defined for
 * each of these views.
 */
@ExperimentalPreviewMapboxNavigationAPI
class ViewBinderCustomization {

    /**
     * Customize the speed limit view by providing your own [UIBinder].
     * Use [UIBinder.USE_DEFAULT] to reset to default.
     */
    var speedLimitBinder: UIBinder? = null

    /**
     * Customize the maneuvers view by providing your own [UIBinder].
     * Use [UIBinder.USE_DEFAULT] to reset to default.
     */
    var maneuverBinder: UIBinder? = null

    /**
     * Customize the road name view by providing your own [UIBinder].
     * Use [UIBinder.USE_DEFAULT] to reset to default.
     */
    var roadNameBinder: UIBinder? = null

    /**
     * Customize the Info Panel Trip Progress by providing your own [UIBinder].
     * Use [UIBinder.USE_DEFAULT] to reset to default.
     */
    var infoPanelTripProgressBinder: UIBinder? = null

    /**
     * Customize the Info Panel Header by providing your own [UIBinder].
     * Use [UIBinder.USE_DEFAULT] to reset to default.
     */
    var infoPanelHeaderBinder: UIBinder? = null

    /**
     * Customize the Info Panel Content by providing your own [UIBinder].
     * Use [UIBinder.USE_DEFAULT] to reset to default.
     */
    var infoPanelContentBinder: UIBinder? = null

    /**
     * Customize the Action Buttons by providing your own [UIBinder].
     * Use [UIBinder.USE_DEFAULT] to reset to default.
     */
    var actionButtonsBinder: UIBinder? = null
}
