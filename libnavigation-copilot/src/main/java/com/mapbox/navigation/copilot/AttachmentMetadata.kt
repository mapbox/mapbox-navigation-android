package com.mapbox.navigation.copilot

import androidx.annotation.Keep
import com.mapbox.navigation.copilot.DateUtils.obtainCurrentDate

@Keep
internal data class AttachmentMetadata(
    val name: String,
    val created: String = obtainCurrentDate(),
    val fileId: String,
    val format: String,
    val type: String,
    val sessionId: String,
    var size: Int? = null,
    var startTime: String? = null,
    var endTime: String? = null,
)
