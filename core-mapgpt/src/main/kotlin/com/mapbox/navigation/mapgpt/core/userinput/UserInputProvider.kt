package com.mapbox.navigation.mapgpt.core.userinput

import com.mapbox.navigation.mapgpt.core.MiddlewareProvider

/**
 * Used to identify your UserInputMiddleware.
 *
 * @param key A unique key identifying and persisting state. The key is not meant for display in
 *     case there is a need to translate display names to different languages.
 */
abstract class UserInputProvider(key: String): MiddlewareProvider(key) {
    object SpeechRecognizer : UserInputProvider("speech_recognizer")
    object GoogleCloudPlatform : UserInputProvider("google_cloud_platform")
    object PicovoiceCheetah : UserInputProvider("picovoice_cheetah")
    object PicovoiceLeopard : UserInputProvider("picovoice_leopard")
    object MapboxASR : UserInputProvider("mapbox_asr")

    override fun toString(): String = "UserInputProvider(key=$key)"
}
