package com.mapbox.navigation.core.internal.dump

/**
 * This is used for unit testing the [MapboxDumpRegistry] singleton.
 */
internal class MapboxDumpRegistryDelegate {
    private val interceptors = mutableSetOf<MapboxDumpInterceptor>()

    var defaultInterceptor: MapboxDumpInterceptor? = null
        set(value) {
            field?.let { this.interceptors.remove(it) }
            value?.let { addInterceptors(it) }
            field = value
        }

    init {
        defaultInterceptor = HelpDumpInterceptor()
    }

    fun addInterceptors(vararg interceptors: MapboxDumpInterceptor) =
        this.interceptors.addAll(interceptors)

    fun getInterceptors(): List<MapboxDumpInterceptor> = interceptors.toList()

    fun getInterceptors(command: String): List<MapboxDumpInterceptor> =
        interceptors.filter { it.command() == command }

    fun removeInterceptors(vararg interceptors: MapboxDumpInterceptor) {
        if (interceptors.contains(defaultInterceptor)) {
            defaultInterceptor = null
        }
        this.interceptors.removeAll(interceptors.toSet())
    }
}
