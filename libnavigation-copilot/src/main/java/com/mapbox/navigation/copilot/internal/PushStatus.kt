package com.mapbox.navigation.copilot.internal

/**
 * PushStatus
 *
 * @property metadata [CopilotSession]
 */
sealed class PushStatus {

    abstract val metadata: CopilotSession

    /**
     * Success
     *
     * @property metadata [CopilotSession]
     */
    data class Success(override val metadata: CopilotSession) : PushStatus()

    /**
     * Failed
     *
     * @property metadata [CopilotSession]
     */
    data class Failed(override val metadata: CopilotSession) : PushStatus()
}
