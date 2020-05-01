package com.mapbox.navigation.ui.alert

/**
 * Immutable object which contains all the required information for a business logic to process.
 */
internal sealed class AlertAction {
    internal data class SetAlertText(val alertText: String) : AlertAction()
    internal data class SetDismissDuration(val dismissDuration: Long) : AlertAction()
}
