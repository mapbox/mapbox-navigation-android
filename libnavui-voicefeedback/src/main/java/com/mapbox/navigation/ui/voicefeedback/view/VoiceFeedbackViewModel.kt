package com.mapbox.navigation.ui.voicefeedback.view

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.voicefeedback.ASRState
import com.mapbox.navigation.voicefeedback.FeedbackAgentSession
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch

@ExperimentalPreviewMapboxNavigationAPI
internal class VoiceFeedbackViewModel(
    private val session: FeedbackAgentSession = FeedbackAgentSession.getRegisteredInstance(),
) : ViewModel() {

    val state: StateFlow<ASRState?> get() = session.asrState

    private val isDialogVisible = MutableStateFlow(false)

    init {
        session.connect()
    }

    fun onVoiceFeedbackButtonClicked() {
        viewModelScope.launch {
            // wait until session is idle and dialog is visible before starting microphone
            combine(session.asrState, isDialogVisible) { state, isDialogVisible ->
                state to isDialogVisible
            }.firstOrNull { (state, isDialogVisible) ->
                state != null && isDialogVisible
            }

            session.startListening()
        }
    }

    fun onCloseDialogButtonClicked() {
        session.interruptListening()
    }

    fun onDialogVisible() {
        isDialogVisible.value = true
    }

    fun onDialogDismissed() {
        isDialogVisible.value = false
    }
}
