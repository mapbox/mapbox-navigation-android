package com.mapbox.navigation.core.telemetry

import java.util.ArrayDeque

data class ItemAccumulationEventDescriptor<T>(val preEventBuffer: ArrayDeque<T>, val postEventBuffer: ArrayDeque<T>, val onBufferFull: (ArrayDeque<T>, ArrayDeque<T>) -> Unit)
