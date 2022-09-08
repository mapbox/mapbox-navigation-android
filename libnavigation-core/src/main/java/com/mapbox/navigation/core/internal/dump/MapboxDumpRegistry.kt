package com.mapbox.navigation.core.internal.dump

/**
 * This is a singleton that allows downstream solutions to define their own interceptors.
 */
object MapboxDumpRegistry {
    private val delegate = MapboxDumpRegistryDelegate()

    /**
     * The default interceptor is usually the [HelpDumpInterceptor]. If you decide to override
     * this, note that it will receive the empty command.
     */
    var defaultInterceptor: MapboxDumpInterceptor?
        get() = delegate.defaultInterceptor
        set(value) {
            delegate.defaultInterceptor = value
        }

    /**
     * Add interceptors.
     */
    fun addInterceptors(vararg interceptors: MapboxDumpInterceptor) =
        delegate.addInterceptors(*interceptors)

    /**
     * Get all available interceptors.
     */
    fun getInterceptors(): List<MapboxDumpInterceptor> =
        delegate.getInterceptors()

    /**
     * Get all available interceptors that can handle the [command].
     */
    fun getInterceptors(command: String): List<MapboxDumpInterceptor> =
        delegate.getInterceptors(command)

    /**
     * Remove interceptors. Note that this can remove the [defaultInterceptor] if you choose to
     * use [getInterceptors] to [removeInterceptors].
     */
    fun removeInterceptors(vararg interceptors: MapboxDumpInterceptor) =
        delegate.removeInterceptors(*interceptors)
}
