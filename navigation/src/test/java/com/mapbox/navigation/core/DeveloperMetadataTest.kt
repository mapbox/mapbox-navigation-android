package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import org.junit.Assert.assertEquals
import org.junit.Test

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
class DeveloperMetadataTest {

    @Test
    fun `copy with no arguments`() {
        val originalCopilotSessionId = "123-123"
        val original = DeveloperMetadata(originalCopilotSessionId)

        val result = original.copy()

        assertEquals(originalCopilotSessionId, result.copilotSessionId)
    }

    @Test
    fun `copy with arguments`() {
        val originalCopilotSessionId = "123-123"
        val newCopilotSessionId = "456-456"
        val original = DeveloperMetadata(originalCopilotSessionId)

        val result = original.copy(copilotSessionId = newCopilotSessionId)

        assertEquals(newCopilotSessionId, result.copilotSessionId)
    }
}
