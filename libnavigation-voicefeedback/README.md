# Voice Feedback (for Nav SDK v2)

An SDK for voice-based feedback collection during navigation sessions in Android, powered by MapGPT.

The root package contains ASR engine implementations, state management, context providers, and microphone handling. Supports speech recognition for capturing and processing user voice feedback during navigation.

## Usage

```kotlin
val mapboxNavigation = ..
val feedbackAgent = FeedbackAgentSession.Builder().build()
mapboxNavigation.registerObserver(feedbackAgent)

fun onConnectButtonClick() {
    feedbackAgent.connect()
}

fun onMicButtonClick() {
    feedbackAgent.startListening()
}

feedbackAgent.asrState
    .collect { state ->
        when (state) {
            is ASRState.Listening -> {
                transcript = state.text
            }
            is ASRState.Result -> {
                mapboxNavigation.postVoiceFeedback(
                    feedbackSubType = state.feedbackType,
                    description = state.text,
                    screenshot = "",
                ) {
                    // feedback submitted
                }
            }
        }
    }
```

#### Key Components:
- **AutomaticSpeechRecognitionEngine**: Interface defining the contract for speech recognition implementations with state flow exposure for reactive recognition updates
- **AsrSessionState**: Sealed class representing connection states including `Disconnected`, `Connecting`, `Connected`.
- **ASRState**: Sealed interface representing recognition states including `Idle`, `Listening`, `Error`, `Result`, `NoResult`, `SpeechFinishedWaitingForResult`, `Interrupted`, and `InterruptedByTimeout`
- **FeedbackAgentContextProvider**: Interface for retrieving voice feedback context data during feedback sessions

## Key Architecture Decisions

### Microphone Abstraction as Internal API

ASR requires an audio source. The module uses the Android AudioRecord by default. E2E tests need to inject audio from files (e.g., InputStreamMicrophone) to circumvent real microphone access in tests.

Keep the Microphone interface and injection point internal to the module. The public FeedbackAgentOptions.create() exposes only language and endpoint to keep the public API simple. A custom microphone can be passed via the internal constructor for testing.

- Public API stays minimal; clients use defaults.
- Testing can use InputStreamMicrophone for deterministic E2E tests.
- Microphone configuration (e.g., custom sources, sample rate) may be exposed in the future if customers request it.

### Manual Connection (connect() Required)

FeedbackAgentSession registers as a MapboxNavigationObserver/UIComponent but does not connect to the ASR service automatically on onAttached().

Require the client to call connect() explicitly after registering the session.

Allows client to control the connection state and reduce resource usage and avoid idle WebSocket connections.

### Internal FeedbackAgentContextProvider

Every ASR request sends context (user location, route state, app settings, etc.) to the MapGPT backend.

FeedbackAgentContextProvider derives the appropriate context from `MapboxNavigation`.

The context is required by the backend to provide the LLM with the necessary tokens to accurately submit feedback.