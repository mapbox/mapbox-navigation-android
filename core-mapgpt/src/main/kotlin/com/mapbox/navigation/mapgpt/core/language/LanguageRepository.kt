package com.mapbox.navigation.mapgpt.core.language

import com.mapbox.navigation.mapgpt.core.textplayer.DefaultVoice
import com.mapbox.navigation.mapgpt.core.textplayer.Voice
import kotlinx.coroutines.flow.MutableStateFlow

interface LanguageRepository {
    val initialVoice: Voice
    val initialLanguage: Language

    val availableLanguages: MutableStateFlow<Set<Language>>
    val availableVoices: MutableStateFlow<Set<Voice>>

    val language: MutableStateFlow<Language>
    val voice: MutableStateFlow<Voice>
}

class LanguageRepositoryImpl(
    override val initialVoice: Voice = DefaultVoice,
    override val initialLanguage: Language = deviceLanguage(),
) : LanguageRepository {

    init {
        println("LanguageRepository has been created with ${initialLanguage.languageTag}, $initialVoice")
    }

    override val availableVoices = MutableStateFlow<Set<Voice>>(emptySet())
    override val availableLanguages = MutableStateFlow<Set<Language>>(emptySet())
    override val voice = MutableStateFlow(initialVoice)
    override val language = MutableStateFlow(initialLanguage)
}
