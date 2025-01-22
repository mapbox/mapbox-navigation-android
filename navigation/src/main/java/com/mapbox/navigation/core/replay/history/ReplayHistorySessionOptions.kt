package com.mapbox.navigation.core.replay.history

import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.core.MapboxNavigation

/**
 * Options to use with the [ReplayHistorySession].
 *
 * @param filePath absolute path to the history file
 * @param replayHistoryMapper converts history events into replayable events
 * @param enableSetRoute relays the set route events into [MapboxNavigation.setNavigationRoutes]
 */
@ExperimentalPreviewMapboxNavigationAPI
class ReplayHistorySessionOptions private constructor(
    val filePath: String?,
    val replayHistoryMapper: ReplayHistoryMapper,
    val enableSetRoute: Boolean,
) {
    /**
     * @return the builder that created the [ReplayHistorySessionOptions]
     */
    fun toBuilder(): Builder = Builder().apply {
        filePath(filePath)
        replayHistoryMapper(replayHistoryMapper)
        enableSetRoute(enableSetRoute)
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplayHistorySessionOptions

        if (filePath != other.filePath) return false
        if (replayHistoryMapper != other.replayHistoryMapper) return false
        if (enableSetRoute != other.enableSetRoute) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = filePath.hashCode()
        result = 31 * result + replayHistoryMapper.hashCode()
        result = 31 * result + enableSetRoute.hashCode()
        return result
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun toString(): String {
        return "ReplayHistorySessionOptions(" +
            "filePath=$filePath, " +
            "replayHistoryMapper=$replayHistoryMapper, " +
            "enableSetRoute=$enableSetRoute" +
            ")"
    }

    /**
     * Used to build [ReplayHistorySession].
     */
    @ExperimentalPreviewMapboxNavigationAPI
    class Builder {
        private var filePath: String? = null
        private var replayHistoryMapper = ReplayHistoryMapper.Builder()
            .setRouteMapper {
                ReplaySetNavigationRoute.Builder(it.eventTimestamp)
                    .route(it.navigationRoute)
                    .build()
            }
            .build()
        private var enableSetRoute: Boolean = true

        /**
         * Build your [ReplayHistorySessionOptions].
         *
         * @return [ReplayHistorySessionOptions]
         */
        fun build(): ReplayHistorySessionOptions = ReplayHistorySessionOptions(
            filePath = filePath,
            replayHistoryMapper = replayHistoryMapper,
            enableSetRoute = enableSetRoute,
        )

        /**
         * Set a path to the history file.
         *
         * @param filePath absolute path to the history file.
         * @return [Builder]
         */
        fun filePath(filePath: String?): Builder = apply {
            this.filePath = filePath
        }

        /**
         * Set the [ReplayHistoryMapper]. Converts history events into replayable events.
         *
         * @param replayHistoryMapper [ReplayHistoryMapper]
         * @return [Builder]
         */
        fun replayHistoryMapper(replayHistoryMapper: ReplayHistoryMapper): Builder = apply {
            this.replayHistoryMapper = replayHistoryMapper
        }

        /**
         * Relays the set route events into [MapboxNavigation.setNavigationRoutes]
         *
         * @param enableSetRoute
         * @return [Builder]
         */
        fun enableSetRoute(enableSetRoute: Boolean): Builder = apply {
            this.enableSetRoute = enableSetRoute
        }
    }
}
