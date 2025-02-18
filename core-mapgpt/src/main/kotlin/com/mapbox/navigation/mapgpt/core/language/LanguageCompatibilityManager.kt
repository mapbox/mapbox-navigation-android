package com.mapbox.navigation.mapgpt.core.language

import com.mapbox.navigation.mapgpt.core.CoroutineMiddleware
import com.mapbox.navigation.mapgpt.core.MapGptCoreContext
import com.mapbox.navigation.mapgpt.core.textplayer.DefaultVoice
import com.mapbox.navigation.mapgpt.core.textplayer.Voice
import com.mapbox.navigation.mapgpt.core.textplayer.VoicePlayer
import com.mapbox.navigation.mapgpt.core.userinput.UserInputOwner
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach

class LanguageCompatibilityManager(
    private val userInputOwner: UserInputOwner,
    private val voicePlayer: VoicePlayer,
) : CoroutineMiddleware<MapGptCoreContext>() {
    override fun onAttached(middlewareContext: MapGptCoreContext) {
        super.onAttached(middlewareContext)
        combine(
            userInputOwner.availableLanguages,
            voicePlayer.availableLanguages,
        ) { inputLanguages, voiceLanguages -> voiceLanguages.intersect(inputLanguages) }
            .onEach { middlewareContext.onAvailableLanguagesChanged(it) }
            .launchIn(mainScope)
        voicePlayer.availableVoices
            .onEach { middlewareContext.onAvailableVoicesChanged(it) }
            .launchIn(mainScope)
    }

    override fun onDetached(middlewareContext: MapGptCoreContext) {
        super.onDetached(middlewareContext)
        middlewareContext.languageRepository.availableLanguages.value = emptySet()
        middlewareContext.languageRepository.availableVoices.value = emptySet()
    }

    private fun MapGptCoreContext.onAvailableLanguagesChanged(languages: Set<Language>) {
        val modifiedLanguages = languages.toMutableSet()

        val initialLanguage = languageRepository.initialLanguage
        if (!languages.contains(initialLanguage)) {
            log.w(TAG) {
                "Device language ${initialLanguage.languageTag} is not available. " +
                    "The SDK is making it available only because the user's device is set for " +
                    "this language."
            }
            modifiedLanguages.add(initialLanguage)
        }

        val selectedLanguage = languageRepository.language.value
        if (!languages.contains(selectedLanguage)) {
            log.w(TAG) {
                "Selected language ${selectedLanguage.languageTag} is not available. " +
                    "The SDK is making it available only because it has been selected."
            }
            modifiedLanguages.add(selectedLanguage)
        }

        log.i(TAG) {
            "onAvailableLanguagesChanged: ${modifiedLanguages.joinToString { it.languageTag }}"
        }
        languageRepository.availableLanguages.value = modifiedLanguages.toSet()
    }

    private fun MapGptCoreContext.onAvailableVoicesChanged(voices: Set<Voice>) {
        val currentVoice = languageRepository.voice.value
        languageRepository.availableVoices.value = voices
        if (currentVoice == DefaultVoice && voices.isNotEmpty()) {
            val firstVoice = voices.first()
            val initialVoice = languageRepository.initialVoice
            val defaultVoice = if (voices.contains(initialVoice)) initialVoice else firstVoice
            log.i(TAG) { "Setting $defaultVoice for the default voice." }
            languageRepository.voice.value = defaultVoice
        } else if (!voices.safeContains(currentVoice) && currentVoice != DefaultVoice) {
            val availableVoices = languageRepository.availableVoices.value
            val initialVoice = languageRepository.initialVoice
            val voice = if (availableVoices.safeContains(initialVoice)) {
                initialVoice
            } else {
                val availableVoice = availableVoices.firstOrNull() ?: DefaultVoice
                availableVoice
            }
            log.w(TAG) { "$currentVoice is not available. Switching to $voice instead." }
            languageRepository.voice.value = voice
        }
    }

    /**
     * Prevents crashes when using java.util.TreeMap or java.util.TreeSet with custom comparators
     * to sort voices.
     *
     * For example, voices can be set to a sorted set using the following code:
     * val voiceCompare = compareBy<DashVoice> { it.personaName }
     * return voiceMap.values.toSortedSet(voiceCompare)
     */
    private fun Set<Voice>.safeContains(voice: Voice): Boolean {
        return firstOrNull { other -> voice == other } != null
    }

    private companion object {
        private const val TAG = "LanguageCompatibilityManager"
    }
}
