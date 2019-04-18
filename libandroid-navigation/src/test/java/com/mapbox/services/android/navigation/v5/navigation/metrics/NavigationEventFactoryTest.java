package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.location.Location;

import com.mapbox.android.telemetry.TelemetryUtils;
import com.mapbox.geojson.Point;
import com.mapbox.services.android.navigation.v5.routeprogress.MetricsRouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NavigationEventFactoryTest {
  private static final String APP_STATE = "PHONE STATE";
  private static final String AUDIO_TYPE = "AUDIO TYPE";
  private static final int BATTERY_LEVEL = 100;
  private static final String CONNECTIVITY = "CONNECTED";
  private static final int SCREEN_BRIGHTESS = 10;
  private static final int VOLUME_LEVEL = 5;
  private static final String CREATED = "2019-04-18";
  private static final Double LATITUDE_BEFORE = 100.0;
  private static final Double LONGITUDE_BEFORE = 50.0;
  private static final Double LATITUDE_AFTER = 103.0;
  private static final Double LONGITUDE_AFTER = 53.0;
  private static final Double ROUTE_COMPLETED = 1000.0;
  private static final String SESSION_ID = "SESSION ID";
  private static final String GEOMETRY = "GEOMETRY";
  private static final String LOCATION_ENGINE = "GPS";
  private static final String TRIP_ID = "TRIP ID";
  private static final int REROUTE_COUNT = 11;
  private static final String ORIG_REQUEST_ID = "ORI REQ ID";
  private static final String REQUEST_ID = "REQ ID";
  private static final String ORIG_GEO = "ORIG GEO";
  private static final int ORIG_DIS = 50;
  private static final int ORIG_DURA = 10000;
  private static final int ORIG_STEP_COUNT = 80;
  private static final int PERCENT_IN_FOREGROUND = 20;
  private static final int PERCENT_IN_PORTRAIT = 50;
  private static final int CURRENT_STEP_COUNT = 70;
  private static final int SECOND_SINCE_LAST_REROUTE = 5;
  private static final int DISTANCE_REMAINING = 33;
  private static final int DURATION_REMAINING = 1000;
  private static final String DIRECTION_PROFILE = "DIRECTION PROFILE";
  private static final int LEG_INDEX = 5;
  private static final int LEG_COUNT = 15;
  private static final int STEP_INDEX = 4;
  private static final int STEP_COUNT = 6;
  private static final int DIRECTION_DISTANCE = 101;
  private static final int DIRECTION_DURATION = 2100;
  private static final String UPCOMING_INSTRU = "UPCOMING INSTRU";
  private static final String UPCOMING_MODIFIER = "UPCOMING MODIFIER";
  private static final String UPCOMING_NAME = "UPCOMING NAME";
  private static final String UPCOMING_TYPE = "UPCOMING TYPE";
  private static final String PRE_INSTRU = "PRE INSTRU";
  private static final String PRE_MODIFIER = "PRE MODIFIER";
  private static final String PRE_TYPE = "PRE TYPE";
  private static final String PRE_NAME = "PRE_NAME";
  private static final int CURRENT_DISTANCE = 200;
  private static final int CURRENT_DURATION = 2001;
  private static final String SDK_ID = "SDK ID";
  private static final Boolean BATTER_PLUGGEDIN = Boolean.TRUE;
  public static final boolean IS_MOCK = true;
  private static final int DISTANCE_TRAVELED = 500;
  private static final String FEEDBACK_ID = "100";
  private static final String USER_ID = "110";
  private Date date;
  private PhoneState phoneState;
  private SessionState sessionState;
  private MetricsRouteProgress metricsRouteProgress;
  private Location locationBefore, locationAfter;

  @Before
  public void setUp() {
    date = new Date();
    phoneState = mock(PhoneState.class);
    when(phoneState.getApplicationState()).thenReturn(APP_STATE);
    when(phoneState.getAudioType()).thenReturn(AUDIO_TYPE);
    when(phoneState.getBatteryLevel()).thenReturn(BATTERY_LEVEL);
    when(phoneState.getConnectivity()).thenReturn(CONNECTIVITY);
    when(phoneState.getScreenBrightness()).thenReturn(SCREEN_BRIGHTESS);
    when(phoneState.getVolumeLevel()).thenReturn(VOLUME_LEVEL);
    when(phoneState.getCreated()).thenReturn(CREATED);
    when(phoneState.isBatteryPluggedIn()).thenReturn(BATTER_PLUGGEDIN);
    when(phoneState.getFeedbackId()).thenReturn(FEEDBACK_ID);
    when(phoneState.getUserId()).thenReturn(USER_ID);

    locationBefore = mock(Location.class);
    when(locationBefore.getLatitude()).thenReturn(LATITUDE_BEFORE);
    when(locationBefore.getLongitude()).thenReturn(LONGITUDE_BEFORE);
    locationAfter = mock(Location.class);
    when(locationAfter.getLatitude()).thenReturn(LATITUDE_AFTER);
    when(locationAfter.getLongitude()).thenReturn(LONGITUDE_AFTER);

    sessionState = mock(SessionState.class);
    when(sessionState.eventRouteDistanceCompleted()).thenReturn(ROUTE_COMPLETED);
    when(sessionState.startTimestamp()).thenReturn(date);
    when(sessionState.sessionIdentifier()).thenReturn(SESSION_ID);
    when(sessionState.currentGeometry()).thenReturn(GEOMETRY);
    when(sessionState.mockLocation()).thenReturn(IS_MOCK);
    when(sessionState.locationEngineName()).thenReturn(LOCATION_ENGINE);
    when(sessionState.tripIdentifier()).thenReturn(TRIP_ID);
    when(sessionState.rerouteCount()).thenReturn(REROUTE_COUNT);
    when(sessionState.originalRequestIdentifier()).thenReturn(ORIG_REQUEST_ID);
    when(sessionState.requestIdentifier()).thenReturn(REQUEST_ID);
    when(sessionState.originalGeometry()).thenReturn(ORIG_GEO);
    when(sessionState.originalDistance()).thenReturn(ORIG_DIS);
    when(sessionState.originalDuration()).thenReturn(ORIG_DURA);
    when(sessionState.originalStepCount()).thenReturn(ORIG_STEP_COUNT);
    when(sessionState.percentInForeground()).thenReturn(PERCENT_IN_FOREGROUND);
    when(sessionState.percentInPortrait()).thenReturn(PERCENT_IN_PORTRAIT);
    when(sessionState.currentStepCount()).thenReturn(CURRENT_STEP_COUNT);
    ArrayList<Location> afterList = new ArrayList<>();
    afterList.add(locationAfter);
    ArrayList<Location> beforeList = new ArrayList<>();
    beforeList.add(locationBefore);
    when(sessionState.afterEventLocations()).thenReturn(afterList);
    when(sessionState.beforeEventLocations()).thenReturn(beforeList);
    when(sessionState.secondsSinceLastReroute()).thenReturn(SECOND_SINCE_LAST_REROUTE);
    when(sessionState.arrivalTimestamp()).thenReturn(date);

    metricsRouteProgress = mock(MetricsRouteProgress.class);
    when(metricsRouteProgress.getDistanceTraveled()).thenReturn(DISTANCE_TRAVELED);
    when(metricsRouteProgress.getDistanceRemaining()).thenReturn(DISTANCE_REMAINING);
    when(metricsRouteProgress.getDurationRemaining()).thenReturn(DURATION_REMAINING);
    when(metricsRouteProgress.getDirectionsRouteProfile()).thenReturn(DIRECTION_PROFILE);
    when(metricsRouteProgress.getLegIndex()).thenReturn(LEG_INDEX);
    when(metricsRouteProgress.getLegCount()).thenReturn(LEG_COUNT);
    when(metricsRouteProgress.getStepIndex()).thenReturn(STEP_INDEX);
    when(metricsRouteProgress.getStepCount()).thenReturn(STEP_COUNT);
    when(metricsRouteProgress.getDirectionsRouteDistance()).thenReturn(DIRECTION_DISTANCE);
    when(metricsRouteProgress.getDirectionsRouteDuration()).thenReturn(DIRECTION_DURATION);
    when(metricsRouteProgress.getUpcomingStepInstruction()).thenReturn(UPCOMING_INSTRU);
    when(metricsRouteProgress.getUpcomingStepModifier()).thenReturn(UPCOMING_MODIFIER);
    when(metricsRouteProgress.getUpcomingStepName()).thenReturn(UPCOMING_NAME);
    when(metricsRouteProgress.getUpcomingStepType()).thenReturn(UPCOMING_TYPE);
    when(metricsRouteProgress.getPreviousStepInstruction()).thenReturn(PRE_INSTRU);
    when(metricsRouteProgress.getPreviousStepModifier()).thenReturn(PRE_MODIFIER);
    when(metricsRouteProgress.getPreviousStepType()).thenReturn(PRE_TYPE);
    when(metricsRouteProgress.getPreviousStepName()).thenReturn(PRE_NAME);
    when(metricsRouteProgress.getCurrentStepDistance()).thenReturn(CURRENT_DISTANCE);
    when(metricsRouteProgress.getCurrentStepDuration()).thenReturn(CURRENT_DURATION);
    when(metricsRouteProgress.getDirectionsRouteDestination())
      .thenReturn(Point.fromLngLat(LONGITUDE_AFTER, LATITUDE_AFTER));
  }

  @Test
  public void testCancelEvent() {
    NavigationCancelEvent cancelEvent = NavigationEventFactory
      .buildNavigationCancelEvent(phoneState, sessionState, metricsRouteProgress, locationBefore, SDK_ID);
    checkNavigationEvent(cancelEvent);
    assertEquals(TelemetryUtils.generateCreateDateFormatted(date), cancelEvent.getArrivalTimestamp());
  }

  @Test
  public void testArriveEvent() {
    NavigationArriveEvent cancelEvent = NavigationEventFactory
      .buildNavigationArriveEvent(phoneState, sessionState, metricsRouteProgress, locationBefore, SDK_ID);
    checkNavigationEvent(cancelEvent);
  }

  @Test
  public void testRerouteEvent() {
    RerouteEvent rerouteEvent = mock(RerouteEvent.class);
    int newDistanceRemaining = 100;
    when(rerouteEvent.getNewDistanceRemaining()).thenReturn(newDistanceRemaining);
    int newDurationRemaining = 1000;
    when(rerouteEvent.getNewDurationRemaining()).thenReturn(newDurationRemaining);
    String newRouteGeo = "new route geo";
    when(rerouteEvent.getNewRouteGeometry()).thenReturn(newRouteGeo);

    NavigationRerouteEvent navigationRerouteEvent = NavigationEventFactory
      .buildNavigationRerouteEvent(phoneState, sessionState, metricsRouteProgress, locationBefore, SDK_ID, rerouteEvent);
    checkNavigationEvent(navigationRerouteEvent);
    checkNavigationStepEvent(navigationRerouteEvent);
    assertEquals(newDistanceRemaining, navigationRerouteEvent.getNewDistanceRemaining(), 0);
    assertEquals(newDurationRemaining, navigationRerouteEvent.getNewDurationRemaining(), 0);
    assertEquals(newRouteGeo, navigationRerouteEvent.getNewGeometry());
    assertEquals(SECOND_SINCE_LAST_REROUTE, navigationRerouteEvent.getSecondsSinceLastReroute());
    Location[] locationsAfter = navigationRerouteEvent.getLocationsAfter();
    assertEquals(1, locationsAfter.length);
    assertEquals(LONGITUDE_AFTER, locationsAfter[0].getLongitude(), 0);
    assertEquals(LATITUDE_AFTER, locationsAfter[0].getLatitude(), 0);
    Location[] locationsBefore = navigationRerouteEvent.getLocationsBefore();
    assertEquals(1, locationsBefore.length);
    assertEquals(LONGITUDE_BEFORE, locationsBefore[0].getLongitude(), 0);
    assertEquals(LATITUDE_BEFORE, locationsBefore[0].getLatitude(), 0);
    assertEquals(FEEDBACK_ID, navigationRerouteEvent.getFeedbackId());
  }

  @Test
  public void testFeedbackEvent() {
    String description = "This is test description";
    String feedbackType = "feed back type";
    String screenshot = "screenshot";
    String feedbackSource = "source";
    NavigationFeedbackEvent navigationFeedbackEvent = NavigationEventFactory
      .buildNavigationFeedbackEvent(phoneState, sessionState, metricsRouteProgress, locationBefore, SDK_ID,
        description, feedbackType, screenshot, feedbackSource);

    checkNavigationEvent(navigationFeedbackEvent);
    checkNavigationStepEvent(navigationFeedbackEvent);
    Location[] locationsAfter = navigationFeedbackEvent.getLocationsAfter();
    assertEquals(1, locationsAfter.length);
    assertEquals(LONGITUDE_AFTER, locationsAfter[0].getLongitude(), 0);
    assertEquals(LATITUDE_AFTER, locationsAfter[0].getLatitude(), 0);
    Location[] locationsBefore = navigationFeedbackEvent.getLocationsBefore();
    assertEquals(1, locationsBefore.length);
    assertEquals(LONGITUDE_BEFORE, locationsBefore[0].getLongitude(), 0);
    assertEquals(LATITUDE_BEFORE, locationsBefore[0].getLatitude(), 0);
    assertEquals(FEEDBACK_ID, navigationFeedbackEvent.getFeedbackId());
    assertEquals(USER_ID, navigationFeedbackEvent.getUserId());
    assertEquals(description, navigationFeedbackEvent.getDescription());
    assertEquals(feedbackType, navigationFeedbackEvent.getFeedbackType());
    assertEquals(screenshot, navigationFeedbackEvent.getScreenshot());
    assertEquals(feedbackSource, navigationFeedbackEvent.getSource());
  }

  private void checkNavigationStepEvent(NavigationStepEvent event){
    assertEquals(PRE_INSTRU, event.getPreviousInstruction());
    assertEquals(PRE_MODIFIER, event.getPreviousModifier());
    assertEquals(PRE_NAME, event.getPreviousName());
    assertEquals(PRE_TYPE, event.getPreviousType());
    assertEquals(UPCOMING_INSTRU, event.getUpcomingInstruction());
    assertEquals(UPCOMING_MODIFIER, event.getUpcomingModifier());
    assertEquals(UPCOMING_NAME, event.getUpcomingName());
    assertEquals(UPCOMING_TYPE, event.getUpcomingType());
  }
  private void checkNavigationEvent(NavigationEvent event) {
    assertEquals(APP_STATE, event.getApplicationState());
    assertEquals(AUDIO_TYPE, event.getAudioType());
    assertEquals(CONNECTIVITY, event.getConnectivity());
    assertEquals(CREATED, event.getCreated());
    assertEquals(GEOMETRY, event.getGeometry());
    assertEquals(LOCATION_ENGINE, event.getLocationEngine());
    assertEquals(ORIG_GEO, event.getOriginalGeometry());
    assertEquals(BATTER_PLUGGEDIN, event.isBatteryPluggedIn());
    assertEquals(VOLUME_LEVEL, event.getVolumeLevel(), 0);
    assertEquals(SCREEN_BRIGHTESS, event.getScreenBrightness(), 0);
    assertEquals(BATTERY_LEVEL, event.getBatteryLevel(), 0);
    assertEquals(TelemetryUtils.generateCreateDateFormatted(date), event.getStartTimestamp());
    assertEquals(SDK_ID, event.getSdkIdentifier());
    assertEquals(SESSION_ID, event.getSessionIdentifier());
    assertEquals(DIRECTION_PROFILE, event.getProfile());
    assertEquals(ORIG_REQUEST_ID, event.getOriginalRequestIdentifier());
    assertEquals(REQUEST_ID, event.getRequestIdentifier());
    assertEquals(ORIG_GEO, event.getOriginalGeometry());
    assertEquals(AUDIO_TYPE, event.getAudioType());
    assertEquals(TRIP_ID, event.getTripIdentifier());
    assertEquals(LATITUDE_BEFORE, event.getLat(), .0);
    assertEquals(LONGITUDE_BEFORE, event.getLng(), 0);
    assertEquals(IS_MOCK, event.isSimulation());
    assertEquals(DistanceFormatter.calculateAbsoluteDistance(locationBefore, metricsRouteProgress),
      event.getAbsoluteDistanceToDestination());
    assertEquals(PERCENT_IN_PORTRAIT, event.getPercentTimeInPortrait(), 0);
    assertEquals(PERCENT_IN_FOREGROUND, event.getPercentTimeInForeground(), 0);
    assertEquals(ROUTE_COMPLETED + DISTANCE_TRAVELED, event.getDistanceCompleted(), 0);
    assertEquals(DISTANCE_REMAINING, event.getDistanceRemaining(), 0);
    assertEquals(NavigationEventFactory.EVENT_VERSION, event.getEventVersion());
    assertEquals(DIRECTION_DISTANCE, event.getEstimatedDistance(), 0);
    assertEquals(DIRECTION_DURATION, event.getEstimatedDuration(), 0);
    assertEquals(REROUTE_COUNT, event.getRerouteCount(), 0);
    assertEquals(ORIG_DIS, event.getOriginalEstimatedDistance(), 0);
    assertEquals(ORIG_DURA, event.getOriginalEstimatedDuration(), 0);
    assertEquals(STEP_COUNT, event.getStepCount(), 0);
    assertEquals(ORIG_STEP_COUNT, event.getOriginalStepCount(), 0);
    assertEquals(LEG_INDEX, event.getLegIndex(), 0);
    assertEquals(LEG_COUNT, event.getLegCount(), 0);
    assertEquals(STEP_INDEX, event.getStepIndex(), 0);
    assertEquals(CURRENT_STEP_COUNT, event.getTotalStepCount(), 0);

  }
}
