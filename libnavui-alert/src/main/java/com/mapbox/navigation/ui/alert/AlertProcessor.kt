package com.mapbox.navigation.ui.alert

import com.mapbox.navigation.ui.base.model.AlertState

internal typealias SetAlertText<T1, T2> = (input: T1) -> T2

internal typealias SetDismissDuration<T1, T2> = (input: T1) -> T2

internal typealias AlertReducer<T1, T2> = (input1: T1, input2: T2) -> T1

internal val setAlertText: SetAlertText<AlertAction, AlertResult> = { action ->
    if (action is AlertAction.SetAlertText) {
        when (action.alertText.isEmpty()) {
            true -> AlertResult.SetAlertTextResult.Failure(IllegalArgumentException("Text to be shown in alert view cannot be empty"))
            false -> AlertResult.SetAlertTextResult.Success(action.alertText)
        }
    } else {
        AlertResult.SetAlertTextResult.Failure(IllegalArgumentException("Unknown $action passed as input"))
    }
}

internal val setDismissDuration: SetDismissDuration<AlertAction, AlertResult> = { action ->
    if (action is AlertAction.SetDismissDuration) {
        when (action.dismissDuration > 5000L) {
            true -> AlertResult.SetDismissDurationResult.Failure(IllegalArgumentException("Duration to dismiss the Alert View cannot be greater than 5000 milliseconds"))
            false -> AlertResult.SetDismissDurationResult.Success(action.dismissDuration)
        }
    } else {
        AlertResult.SetDismissDurationResult.Failure(IllegalArgumentException("Unknown $action passed as input"))
    }
}

internal val reducer: AlertReducer<AlertState, AlertResult> = { previousState, result ->
    when (result) {
        is AlertResult.SetDismissDurationResult.Success -> {
            previousState.copy(durationToDismiss = result.dismissDuration)
        }
        is AlertResult.SetDismissDurationResult.Failure -> {
            previousState.copy(error = result.error)
        }
        is AlertResult.SetAlertTextResult.Success -> {
            previousState.copy(avText = result.alertText)
        }
        is AlertResult.SetAlertTextResult.Failure -> {
            previousState.copy(error = result.error)
        }
    }
}
