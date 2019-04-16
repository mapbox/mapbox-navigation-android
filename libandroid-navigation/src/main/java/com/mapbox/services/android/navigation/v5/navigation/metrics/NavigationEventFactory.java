package com.mapbox.services.android.navigation.v5.navigation.metrics;

import com.mapbox.android.telemetry.Event;

import java.util.HashMap;
import java.util.Map;

public class NavigationEventFactory {

  private static final String NOT_A_NAVIGATION_EVENT_TYPE = "Type must be a navigation event.";
  private static final String NAVIGATION_STATE_ILLEGAL_NULL = "NavigationState cannot be null.";
  private final Map<Event.Type, NavBuildEvent> BUILD_NAV_EVENT = new HashMap<Event.Type, NavBuildEvent>() {
    {
      put(Event.Type.NAV_ARRIVE, new NavBuildEvent() {
        @Override
        public Event build(NavigationState navigationState) {
          return buildNavigationArriveEvent(navigationState);
        }
      });
      put(Event.Type.NAV_DEPART, new NavBuildEvent() {
        @Override
        public Event build(NavigationState navigationState) {
          return buildNavigationDepartEvent(navigationState);
        }
      });
      put(Event.Type.NAV_CANCEL, new NavBuildEvent() {
        @Override
        public Event build(NavigationState navigationState) {
          return buildNavigationCancelEvent(navigationState);
        }
      });
      put(Event.Type.NAV_FEEDBACK, new NavBuildEvent() {
        @Override
        public Event build(NavigationState navigationState) {
          return buildNavigationFeedbackEvent(navigationState);
        }
      });
      put(Event.Type.NAV_REROUTE, new NavBuildEvent() {
        @Override
        public Event build(NavigationState navigationState) {
          return buildNavigationRerouteEvent(navigationState);
        }
      });
      put(Event.Type.NAV_FASTER_ROUTE, new NavBuildEvent() {
        @Override
        public Event build(NavigationState navigationState) {
          return buildNavigationFasterRouteEvent(navigationState);
        }
      });
    }
  };

  public Event createNavigationEvent(Event.Type type, NavigationState navigationState) {
    check(type, navigationState);
    return BUILD_NAV_EVENT.get(type).build(navigationState);
  }

  private NavigationDepartEvent buildNavigationDepartEvent(NavigationState navigationState) {
    return new NavigationDepartEvent(navigationState);
  }

  private NavigationArriveEvent buildNavigationArriveEvent(NavigationState navigationState) {
    return new NavigationArriveEvent(navigationState);
  }

  private NavigationCancelEvent buildNavigationCancelEvent(NavigationState navigationState) {
    return new NavigationCancelEvent(navigationState);
  }

  private NavigationRerouteEvent buildNavigationRerouteEvent(NavigationState navigationState) {
    return new NavigationRerouteEvent(navigationState);
  }

  private NavigationFeedbackEvent buildNavigationFeedbackEvent(NavigationState navigationState) {
    return new NavigationFeedbackEvent(navigationState);
  }

  private NavigationFasterRouteEvent buildNavigationFasterRouteEvent(NavigationState navigationState) {
    return new NavigationFasterRouteEvent(navigationState);
  }

  private void check(Event.Type type, NavigationState navigationState) {
    isNotNull(navigationState);
  }

  private void isNotNull(NavigationState navigationState) {
    if (navigationState == null) {
      throw new IllegalArgumentException(NAVIGATION_STATE_ILLEGAL_NULL);
    }
  }
}
