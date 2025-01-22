package com.mapbox.navigation.core.replay.history

import com.mapbox.navigation.core.history.MapboxHistoryReader
import com.mapbox.navigation.core.history.MapboxHistoryRecorder
import com.mapbox.navigation.core.history.model.HistoryEvent
import com.mapbox.navigation.core.history.model.HistoryEventGetStatus
import com.mapbox.navigation.core.history.model.HistoryEventPushHistoryRecord
import com.mapbox.navigation.core.history.model.HistoryEventSetRoute
import com.mapbox.navigation.core.history.model.HistoryEventUpdateLocation
import com.mapbox.navigation.core.replay.MapboxReplayer
import com.mapbox.navigation.core.replay.route.ReplayRouteMapper

/**
 * Mapper that can be used with [ReplayHistoryMapper].
 *
 * This class provides an abstraction for mapping [HistoryEvent] into [ReplayEventBase].
 */
fun interface ReplayHistoryEventMapper<Event : HistoryEvent> {

    /**
     * Override to map your own custom events from history files
     * into [ReplayEventBase] for the [MapboxReplayer].
     */
    fun map(event: Event): ReplayEventBase?
}

/**
 * Mapper used to convert [MapboxHistoryReader] events into events replayable by
 * the [MapboxReplayer]. Record navigation history data using the [MapboxHistoryRecorder].
 */
class ReplayHistoryMapper private constructor(
    private val locationMapper: ReplayHistoryEventMapper<HistoryEventUpdateLocation>?,
    private val setRouteMapper: ReplayHistoryEventMapper<HistoryEventSetRoute>?,
    private val statusMapper: ReplayHistoryEventMapper<HistoryEventGetStatus>?,
    private val pushEventMappers: List<ReplayHistoryEventMapper<HistoryEventPushHistoryRecord>>,
) {

    /**
     * @return the builder that created the [ReplayHistoryMapper]
     */
    fun toBuilder(): Builder = Builder().apply {
        locationMapper(locationMapper)
        setRouteMapper(setRouteMapper)
        statusMapper(statusMapper)
        pushEventMappers(pushEventMappers)
    }

    /**
     * Given [HistoryEvent] map to [ReplayEventBase] that can be replayed using [MapboxReplayer]
     *
     * Use the [MapboxHistoryReader] in order to read the event from a file.
     */
    fun mapToReplayEvent(historyEvent: HistoryEvent): ReplayEventBase? {
        return when (historyEvent) {
            is HistoryEventUpdateLocation -> locationMapper?.map(historyEvent)
            is HistoryEventSetRoute -> setRouteMapper?.map(historyEvent)
            is HistoryEventGetStatus -> statusMapper?.map(historyEvent)
            is HistoryEventPushHistoryRecord -> mapPushEvent(historyEvent)
            else -> null
        }
    }

    private fun mapPushEvent(historyEvent: HistoryEventPushHistoryRecord): ReplayEventBase? {
        for (mapper in pushEventMappers) {
            val mappedEvent = mapper.map(historyEvent)
            if (mappedEvent != null) {
                return mappedEvent
            }
        }
        return null
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as ReplayHistoryMapper

        if (locationMapper != other.locationMapper) return false
        if (setRouteMapper != other.setRouteMapper) return false
        if (statusMapper != other.statusMapper) return false
        if (pushEventMappers != other.pushEventMappers) return false

        return true
    }

    /**
     * Regenerate whenever a change is made
     */
    override fun hashCode(): Int {
        var result = locationMapper.hashCode()
        result = 31 * result + setRouteMapper.hashCode()
        result = 31 * result + statusMapper.hashCode()
        result = 31 * result + pushEventMappers.hashCode()
        return result
    }

    /**
     * Returns a string representation of the object.
     */
    override fun toString(): String {
        return "ReplayHistoryMapper(" +
            "locationMapper=$locationMapper, " +
            "setRouteMapper=$setRouteMapper, " +
            "statusMapper=$statusMapper, " +
            "pushEventMappers=$pushEventMappers" +
            ")"
    }

    /**
     * Build a new [ReplayHistoryMapper]
     */
    class Builder {
        private var locationMapper: ReplayHistoryEventMapper<HistoryEventUpdateLocation>? =
            DefaultLocationMapper
        private var setRouteMapper: ReplayHistoryEventMapper<HistoryEventSetRoute>? =
            DefaultSetRouteMapper
        private var statusMapper: ReplayHistoryEventMapper<HistoryEventGetStatus>? =
            DefaultStatusMapper
        private var pushEventMappers =
            emptyList<ReplayHistoryEventMapper<HistoryEventPushHistoryRecord>>()

        /**
         * Build your [ReplayHistoryMapper].
         *
         * @return [ReplayHistoryMapper]
         */
        fun build(): ReplayHistoryMapper {
            return ReplayHistoryMapper(
                locationMapper = locationMapper,
                setRouteMapper = setRouteMapper,
                statusMapper = statusMapper,
                pushEventMappers = pushEventMappers,
            )
        }

        /**
         * Override to create a custom event mapper.
         * Set to `null` to disable the HistoryEventUpdateLocation.
         */
        fun locationMapper(
            locationMapper: ReplayHistoryEventMapper<HistoryEventUpdateLocation>?,
        ): Builder = apply {
            this.locationMapper = locationMapper
        }

        /**
         * Override to create a custom event mapper.
         * Set to `null` to disable the HistoryEventSetRoute.
         */
        fun setRouteMapper(
            setRouteMapper: ReplayHistoryEventMapper<HistoryEventSetRoute>?,
        ): Builder = apply {
            this.setRouteMapper = setRouteMapper
        }

        /**
         * Override to create a custom event mapper.
         * Set to `null` to disable the HistoryEventGetStatus.
         */
        fun statusMapper(
            statusMapper: ReplayHistoryEventMapper<HistoryEventGetStatus>?,
        ): Builder = apply {
            this.statusMapper = statusMapper
        }

        /**
         * Add custom push event mappers. This is empty by default.
         */
        fun pushEventMappers(
            pushEventMappers: List<ReplayHistoryEventMapper<HistoryEventPushHistoryRecord>>,
        ): Builder = apply {
            this.pushEventMappers = pushEventMappers
        }

        private companion object {
            private val DefaultLocationMapper =
                ReplayHistoryEventMapper<HistoryEventUpdateLocation> {
                    ReplayRouteMapper.mapToUpdateLocation(it.eventTimestamp, it.location)
                }

            private val DefaultSetRouteMapper =
                ReplayHistoryEventMapper<HistoryEventSetRoute> {
                    ReplaySetNavigationRoute.Builder(it.eventTimestamp)
                        .route(it.navigationRoute)
                        .build()
                }

            private val DefaultStatusMapper =
                ReplayHistoryEventMapper<HistoryEventGetStatus> {
                    ReplayEventGetStatus(it.eventTimestamp)
                }
        }
    }
}
