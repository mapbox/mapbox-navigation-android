package com.mapbox.navigation.mapgpt.core.audiofocus

/**
 * A class that is used for by different platforms and OS versions. It is used to keep track of
 * which owners have requested audio focus.
 */
internal class AudioFocusRequestHolder {
    private val ownersRequestedFocus = mutableSetOf<AudioFocusOwner>()

    /**
     * Add an owner that has audio focus.
     */
    fun add(owner: AudioFocusOwner) {
        ownersRequestedFocus.add(owner)
    }

    /**
     * Removes the an owner from the focus counter.
     * @return true if the owner has focus.
     */
    fun remove(owners: AudioFocusOwner): Boolean {
        return ownersRequestedFocus.remove(owners)
    }

    /**
     * Returns true if the owner has focus.
     */
    fun hasRequestedFocus(owner: AudioFocusOwner): Boolean {
        return ownersRequestedFocus.contains(owner)
    }
}
