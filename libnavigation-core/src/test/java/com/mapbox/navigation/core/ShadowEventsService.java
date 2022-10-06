package com.mapbox.navigation.core;

import com.mapbox.common.EventsService;
import com.mapbox.common.EventsServerOptions;

import org.robolectric.annotation.Implementation;
import org.robolectric.annotation.Implements;

import static org.mockito.Mockito.mock;

@Implements(EventsService.class)
public class ShadowEventsService {

  @Implementation
  public static EventsService getOrCreate(EventsServerOptions options) {
    return mock(EventsService.class);
  }
}
