package com.mapbox.navigation.copilot.internal

/**
 * PushStatus
 *
 * @property metadata [CopilotMetadata]
 */
sealed class PushStatus {

    abstract val metadata: CopilotMetadata

    /**
     * Success
     *
     * @property metadata [CopilotMetadata]
     */
    data class Success(override val metadata: CopilotMetadata) : PushStatus()

    /**
     * Failed
     *
     * @property metadata [CopilotMetadata]
     */
    data class Failed(override val metadata: CopilotMetadata) : PushStatus()
}
