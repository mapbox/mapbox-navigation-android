package com.mapbox.navigation.dropin.util

import com.mapbox.base.common.logger.model.Message
import com.mapbox.base.common.logger.model.Tag
import com.mapbox.navigation.utils.internal.logD

fun logD(tag: String, message: String) = logD(Tag(tag), Message(message))
