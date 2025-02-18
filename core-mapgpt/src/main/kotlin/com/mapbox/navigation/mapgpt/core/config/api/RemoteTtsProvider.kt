package com.mapbox.navigation.mapgpt.core.config.api

@Target(
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
)
@Retention(AnnotationRetention.BINARY)
annotation class RemoteTtsProvider {
    companion object {
        const val CORE_VOICE = "CORE_VOICE"
        const val MAPGPT_VOICE = "MAPGPT_VOICE"
        const val DISABLED = "DISABLED"

        fun values(): List<String> = listOf(
            CORE_VOICE,
            MAPGPT_VOICE,
            DISABLED,
        )
    }
}
