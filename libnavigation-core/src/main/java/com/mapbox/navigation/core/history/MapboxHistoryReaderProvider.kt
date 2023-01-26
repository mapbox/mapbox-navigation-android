package com.mapbox.navigation.core.history

import androidx.annotation.VisibleForTesting

@VisibleForTesting
internal object MapboxHistoryReaderProvider {
    fun create(filePath: String) = MapboxHistoryReader(filePath)
}
