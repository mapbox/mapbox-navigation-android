package com.mapbox.navigation.base.internal.metric

import com.mapbox.bindgen.Value

fun Value.extractEventsNames(): List<String>? = (this.contents as? List<Value>)
    ?.mapNotNull { (it.contents as? Map<String, Value>)?.get("event")?.contents as? String }
