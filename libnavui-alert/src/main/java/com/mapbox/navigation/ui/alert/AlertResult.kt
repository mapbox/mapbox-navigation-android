package com.mapbox.navigation.ui.alert

/**
 * Immutable object resulting of a processed business logic
 */
internal sealed class AlertResult {

    sealed class SetAlertTextResult : AlertResult() {
        data class Success(val alertText: String) : SetAlertTextResult()
        data class Failure(val error: Throwable) : SetAlertTextResult()
    }

    sealed class SetDismissDurationResult : AlertResult() {
        data class Success(val dismissDuration: Long) : SetDismissDurationResult()
        data class Failure(val error: Throwable) : SetDismissDurationResult()
    }
}
