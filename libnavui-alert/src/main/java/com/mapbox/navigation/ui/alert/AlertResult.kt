package com.mapbox.navigation.ui.alert

/**
 * Immutable object resulting of a processed business logic
 */
internal sealed class AlertResult {

    internal sealed class SetAlertTextResult : AlertResult() {
        internal data class Success(val alertText: String) : SetAlertTextResult()
        internal data class Failure(val error: Throwable) : SetAlertTextResult()
    }

    internal sealed class SetDismissDurationResult : AlertResult() {
        internal data class Success(val dismissDuration: Long) : SetDismissDurationResult()
        internal data class Failure(val error: Throwable) : SetDismissDurationResult()
    }
}
