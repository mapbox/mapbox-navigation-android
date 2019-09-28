package com.mapbox.services.android.navigation.v5.navigation

import com.mapbox.core.utils.TextUtils

private const val EMPTY_STRING = ""

internal class InitialGpsEventFactory @JvmOverloads constructor(private var time: ElapsedTime = ElapsedTime(), private val handler: InitialGpsEventHandler = InitialGpsEventHandler()) {
    private var sessionId = EMPTY_STRING
    private var hasSent: Boolean = false

    fun navigationStarted(sessionId: String) {
        this.sessionId = sessionId
        time.start()
    }

    fun gpsReceived() {
        if (time.start == null) {
            return
        }
        time.end()
        send(time)
    }

    fun reset() {
        time = ElapsedTime()
        hasSent = false
    }

    private fun send(time: ElapsedTime) {
        if (!hasSent && !TextUtils.isEmpty(sessionId)) {
            val elapsedTime = time.elapsedTime
            handler.send(elapsedTime, sessionId)
            hasSent = true
        }
    }

}
