package com.mapbox.services.android.navigation.v5.internal.navigation;

import android.app.Notification;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.content.res.Resources;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.mapbox.api.directions.v5.DirectionsAdapterFactory;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.DirectionsResponse;
import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.StepManeuver;
import com.mapbox.services.android.navigation.v5.BaseTest;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigationOptions;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Locale;
import java.util.Objects;

import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.ROUNDING_INCREMENT_FIVE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_DEPART;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_END_OF_ROAD;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_FORK;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_MERGE;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT;
import static com.mapbox.services.android.navigation.v5.navigation.NavigationConstants.STEP_MANEUVER_TYPE_TURN;
import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SuppressWarnings("KotlinInternalInJava")
public class MapboxNavigationNotificationManeuverInfoTest extends BaseTest {

    private static final String DIRECTIONS_ROUTE_FIXTURE = "directions_v5_precision_6.json";
    private DirectionsRoute route;

    @Before
    public void setUp() throws Exception {
        final String json = loadJsonFixture(DIRECTIONS_ROUTE_FIXTURE);
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(DirectionsAdapterFactory.create()).create();
        DirectionsResponse response = gson.fromJson(json, DirectionsResponse.class);
        route = response.routes().get(0);
    }

    @Test
    public void checksManeuverInfoIsRetrievedForTurnUturnRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_TURN, NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_TURN, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForTurnUturnLeftDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_TURN, NavigationConstants.STEP_MANEUVER_MODIFIER_UTURN,
                NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_TURN, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForArriveLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE, NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForArriveRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE, NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForArriveEmptyRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE, "",
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ARRIVE, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForDepartLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_DEPART, NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_DEPART, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForDepartRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_DEPART, NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_DEPART, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForDepartEmptyRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_DEPART, "",
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_DEPART, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForTurnSharpRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_TURN, NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_TURN, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForTurnRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_TURN, NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_TURN, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForTurnSlightRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_TURN, NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_TURN, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForTurnSharpLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_TURN, NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_TURN, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForTurnLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_TURN, NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_TURN, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForTurnSlightLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_TURN, NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_TURN, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForMergeLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_MERGE, NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_MERGE, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForMergeRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_MERGE, NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_MERGE, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForOffRampLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP, NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForOffRampSlightLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP, NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForOffRampRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP, NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForOffRampSlightRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP, NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_OFF_RAMP, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForForkLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_FORK, NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_FORK, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForForkSlightLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_FORK, NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_FORK, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForForkRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_FORK, NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_FORK, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForForkSlightRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_FORK, NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_FORK, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForForkStraightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_FORK, NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_FORK, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForForkEmptyRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_FORK, "",
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_FORK, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForEndRoadLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_END_OF_ROAD, NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_END_OF_ROAD, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForEndRoadRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_END_OF_ROAD, NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_END_OF_ROAD, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutSharpLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutSlightLeftRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutSharpRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutSlightRightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutStraightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutEmptyRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, "",
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutLeftLeftDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutSharpLeftLeftDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutSlightLeftLeftDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_LEFT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutRightLeftDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutSharpRightLeftDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_SHARP_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutSlightRightLeftDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_SLIGHT_RIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForRoundaboutStraightLeftDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_LEFT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_ROUNDABOUT, mapboxNavigationNotification.getCurrentManeuverType());
    }

    @Test
    public void checksManeuverInfoIsRetrievedForMergeStraightRightDrivingSide() {
        MapboxNavigationNotification mapboxNavigationNotification = buildMapboxNavigationNotification();
        LegStep step = buildLegStep(NavigationConstants.STEP_MANEUVER_TYPE_MERGE, NavigationConstants.STEP_MANEUVER_MODIFIER_STRAIGHT,
                NavigationConstants.STEP_MANEUVER_MODIFIER_RIGHT);
        RouteProgress mockedRouteProgress = buildMockedRouteProgress(step);

        mapboxNavigationNotification.updateNotificationViews(mockedRouteProgress);

        assertEquals(NavigationConstants.STEP_MANEUVER_TYPE_MERGE, mapboxNavigationNotification.getCurrentManeuverType());
    }

    private MapboxNavigationNotification buildMapboxNavigationNotification() {
        MapboxNavigation mockedMapboxNavigation = createMapboxNavigation();
        Context mockedContext = createContext();
        Notification mockedNotification = mock(Notification.class);
        MapboxNavigationNotification notification = new MapboxNavigationNotification(mockedContext,
                mockedMapboxNavigation, mockedNotification);

        MapboxNavigationNotification spyNotification = Mockito.spy(notification);
        doReturn(null).when(spyNotification).getManeuverBitmap(anyString(), anyString(), anyString(), anyFloat());
        return spyNotification;
    }

    private MapboxNavigation createMapboxNavigation() {
        MapboxNavigation mockedMapboxNavigation = mock(MapboxNavigation.class);
        when(mockedMapboxNavigation.getRoute()).thenReturn(route);
        MapboxNavigationOptions mockedMapboxNavigationOptions = mock(MapboxNavigationOptions.class);
        when(mockedMapboxNavigation.options()).thenReturn(mockedMapboxNavigationOptions);
        when(mockedMapboxNavigationOptions.roundingIncrement()).thenReturn(NavigationConstants.ROUNDING_INCREMENT_FIVE);
        return mockedMapboxNavigation;
    }

    private Context createContext() {
        Context mockedContext = mock(Context.class);
        Configuration mockedConfiguration = new Configuration();
        mockedConfiguration.locale = new Locale("en");
        Resources mockedResources = mock(Resources.class);
        when(mockedContext.getResources()).thenReturn(mockedResources);
        when(mockedResources.getConfiguration()).thenReturn(mockedConfiguration);
        PackageManager mockedPackageManager = mock(PackageManager.class);
        when(mockedContext.getPackageManager()).thenReturn(mockedPackageManager);
        when(mockedContext.getString(anyInt())).thenReturn("%s ETA");
        return mockedContext;
    }

    private RouteProgress buildMockedRouteProgress(LegStep step) {
        RouteProgress mockedRouteProgress = mock(RouteProgress.class, RETURNS_DEEP_STUBS);
        when(Objects.requireNonNull(mockedRouteProgress.currentLegProgress()).upComingStep()).thenReturn(step);
        when(mockedRouteProgress.bannerInstruction()).thenReturn(buildBannerInstructions(step.maneuver().type(), step.maneuver().modifier()));

        return mockedRouteProgress;
    }

    private LegStep buildLegStep(String type, String modifier, String drivingSide) {
        LegStep.Builder legStepBuilder = LegStep.fromJson("{\"distance\":56.4,\"duration\":7.5," +
                "\"geometry\":\"ec{bcBjdeqBmByByAaDgAoFWgGXgGhAoF|@wB\",\"name\":\"Saltley Road (A47)\",\"ref\":\"A47\"," +
                "\"mode\":\"driving\",\"maneuver\":{\"location\":[-1.870934,52.492355],\"bearing_before\":60.0," +
                "\"bearing_after\":35.0,\"instruction\":\"Enter the traffic circle and take the 2nd exit onto Saltley Road " +
                "(A47)\",\"type\":\"roundabout\",\"modifier\":\"slight left\",\"exit\":2}," +
                "\"voiceInstructions\":[{\"distanceAlongGeometry\":56.4,\"announcement\":\"Exit the traffic circle onto Saltley" +
                " Road (A47)\",\"ssmlAnnouncement\":\"\\u003cspeak\\u003e\\u003camazon:effect " +
                "name\\u003d\\\"drc\\\"\\u003e\\u003cprosody rate\\u003d\\\"1.08\\\"\\u003eExit the traffic circle onto Saltley" +
                " Road (\\u003csay-as interpret-as\\u003d\\\"address\\\"\\u003eA47\\u003c/say-as\\u003e)" +
                "\\u003c/prosody\\u003e\\u003c/amazon:effect\\u003e\\u003c/speak\\u003e\"}]," +
                "\"bannerInstructions\":[{\"distanceAlongGeometry\":56.4,\"primary\":{\"text\":\"Saltley Road\"," +
                "\"components\":[{\"text\":\"Saltley Road\",\"type\":\"text\",\"abbr\":\"Saltley Rd\",\"abbr_priority\":0}]," +
                "\"type\":\"roundabout\",\"modifier\":\"left\",\"degrees\":290.0,\"driving_side\":\"left\"}," +
                "\"secondary\":{\"text\":\"A47\",\"components\":[{\"text\":\"A47\",\"type\":\"icon\"}],\"type\":\"roundabout\"," +
                "\"modifier\":\"left\"},\"sub\":{\"text\":\"Heartlands Parkway\",\"components\":[{\"text\":\"Heartlands " +
                "Parkway\",\"type\":\"text\",\"abbr\":\"Heartlands Pky\",\"abbr_priority\":0}],\"type\":\"roundabout\"," +
                "\"modifier\":\"left\",\"degrees\":174.0,\"driving_side\":\"left\"}}],\"driving_side\":\"left\",\"weight\":8.6," +
                "\"intersections\":[{\"location\":[-1.870934,52.492355],\"bearings\":[30,195,240],\"entry\":[true,false,false]," +
                "\"in\":2,\"out\":0},{\"location\":[-1.870792,52.492455],\"bearings\":[30,60,225],\"entry\":[true,true,false]," +
                "\"in\":2,\"out\":1},{\"location\":[-1.870288,52.492453],\"bearings\":[135,300,345],\"entry\":[true,false," +
                "false],\"in\":1,\"out\":0}]}").toBuilder();
        StepManeuver stepManeuver = legStepBuilder.build().maneuver().toBuilder()
                .type(type)
                .modifier(modifier)
                .build();
        legStepBuilder
                .maneuver(stepManeuver)
                .drivingSide(drivingSide);

        return legStepBuilder.build();
    }

    private BannerInstructions buildBannerInstructions(String maneuverType, String maneuverModifier) {
        BannerText primaryBanner = BannerText.builder()
                .text("primaryBannerText")
                .type(maneuverType)
                .modifier(maneuverModifier)
                .degrees(60.0)
                .drivingSide("left")
                .build();

        return BannerInstructions.builder()
                .primary(primaryBanner)
                .distanceAlongGeometry(0.3f)
                .build();
    }
}
