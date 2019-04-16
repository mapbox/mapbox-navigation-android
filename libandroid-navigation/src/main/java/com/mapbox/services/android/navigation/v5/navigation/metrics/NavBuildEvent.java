package com.mapbox.services.android.navigation.v5.navigation.metrics;

import com.mapbox.android.telemetry.Event;

interface NavBuildEvent {

  Event build(NavigationState navigationState);
}
