package testapp;

import com.mapbox.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.android.navigation.testapp.test.TestNavigationActivity;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewOptions;
import com.mapbox.services.android.navigation.ui.v5.listeners.SpeechAnnouncementListener;
import com.mapbox.services.android.navigation.ui.v5.map.NavigationMapboxMap;
import com.mapbox.services.android.navigation.ui.v5.voice.SpeechAnnouncement;
import com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation;
import org.junit.Test;
import testapp.activity.BaseNavigationActivityTest;

import static junit.framework.Assert.*;
import static testapp.action.NavigationViewAction.invoke;

public class NavigationViewTest extends BaseNavigationActivityTest implements SpeechAnnouncementListener {

    @Override
    protected Class getActivityClass() {
        return TestNavigationActivity.class;
    }

    @Override
    public SpeechAnnouncement willVoice(SpeechAnnouncement announcement) {
        return SpeechAnnouncement.builder().announcement("All announcements will be the same.").build();
    }

    @Test
    public void onInitialization_navigationMapboxMapIsNotNull() {
        validateTestSetup();

        invoke(getNavigationView(), (uiController, navigationView) -> {
            DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
            NavigationViewOptions options = NavigationViewOptions.builder()
                    .directionsRoute(testRoute)
                    .build();

            navigationView.startNavigation(options);
        });
        NavigationMapboxMap navigationMapboxMap = getNavigationView().retrieveNavigationMapboxMap();

        assertNotNull(navigationMapboxMap);
    }

    @Test
    public void onNavigationStart_mapboxNavigationIsNotNull() {
        validateTestSetup();

        invoke(getNavigationView(), (uiController, navigationView) -> {
            DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
            NavigationViewOptions options = NavigationViewOptions.builder()
                    .directionsRoute(testRoute)
                    .build();

            navigationView.startNavigation(options);
        });
        MapboxNavigation mapboxNavigation = getNavigationView().retrieveMapboxNavigation();

        assertNotNull(mapboxNavigation);
    }

    @Test
    public void onNavigationStart_toggleToMute() {
        validateTestSetup();

        invoke(getNavigationView(), (uiController, navigationView) -> {
            DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
            NavigationViewOptions options = NavigationViewOptions.builder()
                    .directionsRoute(testRoute)
                    .speechAnnouncementListener(this)
                    .build();

            navigationView.startNavigation(options);
            navigationView.toggleMute();
        });
        boolean isMute = getNavigationView().isMuted();

        assertTrue(isMute);
    }

    @Test
    public void muteSpeechPlayer_withoutStartingNavigation() {
        boolean thrown = false;
        try {
            validateTestSetup();

            invoke(getNavigationView(), (uiController, navigationView) -> {
                DirectionsRoute testRoute = DirectionsRoute.fromJson(loadJsonFromAsset("lancaster-1.json"));
                NavigationViewOptions options = NavigationViewOptions.builder()
                        .directionsRoute(testRoute)
                        .speechAnnouncementListener(this)
                        .build();
                navigationView.toggleMute();
                navigationView.startNavigation(options);
            });
        } catch (IllegalStateException exception) {
            thrown = true;
            assertEquals("Navigation needs to start before being able to mute/unmute speech player", exception.getMessage());
        }
        assertTrue(thrown);
    }
}
