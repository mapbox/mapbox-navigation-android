package com.mapbox.navigation.base.performance

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Interface to track performance of your app.
 * This interface can be implemented in order to ingest performance traces with different cloud
 * services. Use the [SharedPerformance.setTraceFactory] to construct your version of [Trace].
 */
@ExperimentalPreviewMapboxNavigationAPI
interface Trace {

    /**
     * Unique name for the trace.
     */
    val name: TraceName

    /**
     * Start the timer for the performance trace.
     */
    fun start(): Trace

    /**
     * Increment a counter as part of your trace.
     */
    fun counter(key: TraceKey, block: (Long) -> Long): Trace

    /**
     * Add an attribute to your trace.
     */
    fun attribute(key: TraceKey, block: (Traceable?) -> TraceValue?): Trace

    /**
     * Add a custom attribute to your trace.
     */
    fun attributeCustom(key: TraceKey, block: (Traceable?) -> Traceable?): Trace

    /**
     * Completes the trace and this trace can be discarded.
     */
    fun stop()

    /**
     * Start the trace with a "source" attribute.
     */
    fun start(source: (Traceable?) -> TraceValue?) = apply {
        attribute(TraceKey.SOURCE, source)
        start()
    }

    /**
     * Stop the trace with a "reason" attribute.
     */
    fun stop(reason: (Traceable?) -> TraceValue?) {
        attribute(TraceKey.REASON, reason)
        stop()
    }
}

/**
 * No-op implementation of [Trace].
 */
@ExperimentalPreviewMapboxNavigationAPI
object NoTrace : Trace {

    override val name: TraceName = TraceName.NONE
    override fun start() = this
    override fun counter(key: TraceKey, block: (Long) -> Long) = this
    override fun attribute(key: TraceKey, block: (Traceable?) -> TraceValue?) = this
    override fun attributeCustom(key: TraceKey, block: (Traceable?) -> Traceable?) = this
    override fun stop() = Unit
}

/**
 * Performance traces have specific naming requirements.
 * This guarantees the values abide by the required format.
 *
 * - Should be in snake_case.
 * - Should be in lowercase.
 * - Should have low indexing.
 */
@ExperimentalPreviewMapboxNavigationAPI
interface Traceable {

    /**
     * Unique value to identify a performance trace metric.
     */
    val snakeCase: String
}

/**
 * Every trace to be measured should have a unique name that is registered as part of this enum.
 */
@ExperimentalPreviewMapboxNavigationAPI
enum class TraceName(
    override val snakeCase: String,
) : Traceable {

    /** Identifies the [NoTrace] */
    NONE("none"),

    /** Time to start playing audio speech from text */
    TEXT_TO_SPEECH_STARTED("text_to_speech_started"),

    /** Microphone is open and no user text is synthesized */
    CONVERSATION_LISTENING_TO_USER("conversation_listening_to_user"),

    /** Microphone is open and text has been synthesized */
    CONVERSATION_USER_SPEAKING("conversation_user_speaking"),

    /** AI is processing the user's input and formulating a response  */
    CONVERSATION_AI_THINKING("conversation_ai_thinking"),

    /** AI has provided text to be played as audio speech */
    CONVERSATION_AI_SPEAKING("conversation_ai_speaking"),

    /** TBT has provided text to be played as audio speech  */
    CONVERSATION_GUIDANCE_SPEAKING("conversation_guidance_speaking"),

    /** User and assistants are not interacting  */
    CONVERSATION_IDLE("conversation_idle"),

    /** User and assistants have been idle for some time */
    CONVERSATION_SLEEPING("conversation_sleeping"),

    /** Conversation is in an error state */
    CONVERSATION_ERROR("conversation_error"),

    /** Conversation is in a non-critical error state (e. g. due to a temporary connection problem) */
    CONVERSATION_NON_CRITICAL_ERROR("conversation_non_critical_error"),

    /** System lacks permission to access the microphone */
    CONVERSATION_NO_MICROPHONE_PERMISSION("conversation_no_microphone_permission"),

    /** Voice assistant feature is turned off or disabled */
    CONVERSATION_DISABLED("conversation_disabled"),
    ;
}

/**
 * Every attribute or counter should have a unique name that is registered as part of this enum.
 */
@ExperimentalPreviewMapboxNavigationAPI
enum class TraceKey(
    override val snakeCase: String,
) : Traceable {

    /** Identifies the source of the trace like (ex: "remote", "local") */
    SOURCE("source"),

    /** Identifies system provider or entities (ex: "spotify", "speech_recognizer") */
    PROVIDER("provider"),

    /** Identifies the reason a trace has ended (ex: "success", "error", "canceled" */
    REASON("reason"),
    ;
}

/**
 * The set of all possible trace values.
 */
@ExperimentalPreviewMapboxNavigationAPI
enum class TraceValue(
    override val snakeCase: String,
) : Traceable {

    /** Attribute to identify successful traces */
    SUCCESS("success"),

    /** Attribute to identify error traces */
    ERROR("error"),

    /** Attribute to identify canceled traces */
    CANCELED("canceled"),

    /** Attribute to identify traces that measure remote calls */
    REMOTE("remote"),

    /** Attribute to identify traces that measure local calls */
    LOCAL("local"),
    ;

    companion object {

        /**
         * Map of [Traceable.snakeCase] to [TraceValue].
         * This is to distinguish [CustomTraceValue] from [TraceValue].
         */
        val valueMap: Map<String, TraceValue> by lazy {
            TraceValue.values().associateBy { it.snakeCase }
        }
    }
}

/**
 * Custom trace value that can be used to add unique values to a trace.
 *
 * @param value identifies a counter or attribute.
 * @throws IllegalArgumentException if the value is not in snake_case or begins with a number.
 */
@ExperimentalPreviewMapboxNavigationAPI
class CustomTraceValue(value: String) : Traceable {

    init {
        require(value.matches(SNAKE_CASE_REGEX)) {
            "Traceable must be in snake_case: $value"
        }
    }

    override val snakeCase: String = value

    private companion object {

        private val SNAKE_CASE_REGEX = "^[a-z]+[a-z0-9_]*\$".toRegex()
    }
}
