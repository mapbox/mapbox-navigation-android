package com.mapbox.navigation.instrumentation_tests.utils.events.verify

import com.google.gson.Gson
import com.mapbox.bindgen.Value
import com.mapbox.navigation.instrumentation_tests.utils.events.domain.EventBase
import org.json.JSONObject

internal fun EventBase.checkIfSubOf(value: Value): List<String> {
    val valueJson = JSONObject(value.toJson())
    val eventJson = JSONObject(Gson().toJson(this))

    val nonValidValues = mutableListOf<String>()
    val subKeys = eventJson.keys()
    subKeys.forEach { eventJsonKey ->
        val eventData = eventJson.get(eventJsonKey)
        val valueData = valueJson.get(eventJsonKey)
        if (eventData != valueData) {
            nonValidValues.add(
                "Event [$event], key [$eventJsonKey] does not match data in `event` [$eventData] and `value` [$valueData]"
            )
        }
    }
    return nonValidValues
}

internal fun Collection<EventBase>.verifyEvents(events: List<Value>): List<String> {
    if (this.size != events.size) {
        return listOf("Events lists must have same sizes: List<EventBase> is ${this.size}, " +
            "List<Value> is ${events.size}")
    }

    return this.foldIndexed(mutableListOf<String>()) { index, accumulateList, eventBase ->
        accumulateList.apply {
            addAll(eventBase.checkIfSubOf(events[index]))
        }
    }
}
