package com.mapbox.navigation.mapgpt.core.userinput

import com.mapbox.navigation.mapgpt.core.MapGptServiceCapabilitiesRepository
import com.mapbox.navigation.mapgpt.core.MiddlewareContext
import com.mapbox.navigation.mapgpt.core.PlatformContext
import com.mapbox.navigation.mapgpt.core.audiofocus.AudioFocusManager
import com.mapbox.navigation.mapgpt.core.microphone.PlatformMicrophone
import com.mapbox.navigation.mapgpt.core.api.MapGptContextProvider
import com.mapbox.navigation.mapgpt.core.api.MapGptService
import com.mapbox.navigation.mapgpt.core.language.Language
import com.mapbox.navigation.mapgpt.core.textplayer.SoundPlayer
import kotlinx.coroutines.flow.StateFlow

class UserInputMiddlewareContext(
    /**
     * Platform specific context.
     */
    val platformContext: PlatformContext,

    /**
     * Provides the ability to use the platform's microphone.
     */
    val microphone: PlatformMicrophone,

    /**
     * The assigned language for the user input.
     */
    val language: StateFlow<Language>,

    /**
     * The current reachability of the device. Returns true if the device is reachable from the
     * internet, false otherwise. Used for detecting when offline services are needed.
     */
    val isReachable: StateFlow<Boolean>,

    /**
     * Provides the ability to request audio focus.
     */
    val audioFocusManager: AudioFocusManager,

    /**
     * Provides the ability to play predetermined audio sounds.
     */
    val soundPlayer: SoundPlayer,

    val mapGptService: MapGptService,

    val mapGptContextProvider: MapGptContextProvider,

    val capabilitiesRepository: MapGptServiceCapabilitiesRepository,

    ) : MiddlewareContext
