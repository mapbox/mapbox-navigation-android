package com.mapbox.navigation.dropin

import com.mapbox.geojson.Point
import com.mapbox.navigation.dropin.lifecycle.UICommand
import com.mapbox.navigation.dropin.lifecycle.UIComponent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch

class UICommandDispatcher: UIComponent() {

    private val _commandFlow = MutableSharedFlow<UICommand>()
    val commandFlow: Flow<UICommand> = _commandFlow

    fun dispatch(command: UICommand) {
        when (command) {
            is UICommand.MapCommand -> {
                onMapCommand(command)
            }
            else -> {
                // no impl
            }
        }
    }

    private fun onMapCommand(command: UICommand.MapCommand) {
        coroutineScope.launch {
            when (command) {
                is UICommand.MapCommand.OnMapLongClicked -> {
                    _commandFlow.emit(
                        UICommand.RoutesCommand.FetchRouteFromCurrentLocation(
                            destination = command.point
                        )
                    )
                }
                is UICommand.MapCommand.OnMapClicked -> {
                    _commandFlow.emit(
                        UICommand.RouteLineCommand.SelectRoute(
                            point = command.point,
                            map = command.map,
                            clickPadding = command.padding
                        )
                    )
                }
            }
        }
    }
}
