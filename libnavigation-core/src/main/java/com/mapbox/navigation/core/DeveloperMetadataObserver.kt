package com.mapbox.navigation.core

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI

/**
 * Callback that provides [DeveloperMetadata].
 * Can be registered via [MapboxNavigation.registerDeveloperMetadataObserver]
 * and unregistered via [MapboxNavigation.unregisterDeveloperMetadataObserver].
 */
@ExperimentalPreviewMapboxNavigationAPI
fun interface DeveloperMetadataObserver {

    /**
     * Invoked whenever [DeveloperMetadata] (its any component) changes.
     * @param metadata the new [DeveloperMetadata]
     */
    fun onDeveloperMetadataChanged(metadata: DeveloperMetadata)
}

/**
 * Contains useful information that should be attached when reporting an issue.
 * Exposed via [DeveloperMetadataObserver.onDeveloperMetadataChanged].
 *
 * @param copilotSessionId an id of a session Copilot uses to track states.
 */
@ExperimentalPreviewMapboxNavigationAPI
class DeveloperMetadata internal constructor(
    val copilotSessionId: String,
) {

    /**
     * Indicates whether some other object is "equal to" this one.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as DeveloperMetadata

        if (copilotSessionId != other.copilotSessionId) return false

        return true
    }

    /**
     * Returns a hash code value for the object.
     */
    override fun hashCode(): Int {
        return copilotSessionId.hashCode()
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "DeveloperMetadata(copilotSessionId='$copilotSessionId')"
    }

    internal fun copy(
        copilotSessionId: String = this.copilotSessionId,
    ): DeveloperMetadata {
        return DeveloperMetadata(copilotSessionId)
    }
}
