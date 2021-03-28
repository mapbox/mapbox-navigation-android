package com.mapbox.navigation.ui.speedlimit.api

import com.mapbox.navigation.base.speed.model.SpeedLimit
import com.mapbox.navigation.ui.base.formatter.ValueFormatter
import com.mapbox.navigation.ui.base.model.Expected
import com.mapbox.navigation.ui.speedlimit.SpeedLimitAction
import com.mapbox.navigation.ui.speedlimit.SpeedLimitProcessor
import com.mapbox.navigation.ui.speedlimit.SpeedLimitResult
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitError
import com.mapbox.navigation.ui.speedlimit.model.UpdateSpeedLimitValue
import com.mapbox.navigation.utils.internal.ifNonNull

/**
 * A Mapbox implementation of the [SpeedLimitApi] for formatting speed limit view related data.
 *
 * @param formatter formats the speed limit data into a string for displaying in the UI
 * @param processor an instance of a [SpeedLimitProcessor]
 */
class MapboxSpeedLimitApi internal constructor(
    var formatter: ValueFormatter<UpdateSpeedLimitValue, String>,
    private val processor: SpeedLimitProcessor
) {

    /**
     * @param formatter formats the speed limit data into a string for displaying in the UI
     */
    constructor(formatter: ValueFormatter<UpdateSpeedLimitValue, String>) : this(
        formatter,
        SpeedLimitProcessor()
    )

    /**
     * Evaluates the [SpeedLimit] data into a state that can be rendered by the view.
     *
     * @param speedLimit a [speedLimit] instance
     * @return an updated state for rendering in the view
     */
    fun updateSpeedLimit(
        speedLimit: SpeedLimit?
    ): Expected<UpdateSpeedLimitValue, UpdateSpeedLimitError> {
        return ifNonNull(speedLimit) { speed ->
            val action = SpeedLimitAction.CalculateSpeedLimitUpdate(speed)
            val result = processor.process(action) as SpeedLimitResult.SpeedLimitCalculation
            Expected.Success(
                UpdateSpeedLimitValue(
                    result.speedKPH,
                    result.speedUnit,
                    result.signFormat,
                    formatter
                )
            )
        } ?: Expected.Failure(
            UpdateSpeedLimitError("Speed Limit data not available", null)
        )
    }
}
