package com.mapbox.navigation.mapgpt.core.textplayer.middleware

import com.mapbox.navigation.mapgpt.core.Middleware
import com.mapbox.navigation.mapgpt.core.textplayer.VoicePlayer

/**
 * Use to provide a custom implementation of [VoicePlayer].
 */
interface VoicePlayerMiddleware : VoicePlayer, Middleware<VoicePlayerMiddlewareContext>
