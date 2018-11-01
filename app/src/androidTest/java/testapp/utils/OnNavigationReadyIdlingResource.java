package testapp.utils;

import android.app.Activity;
import android.os.Handler;
import android.os.Looper;
import android.support.test.espresso.IdlingResource;

import com.mapbox.services.android.navigation.testapp.R;
import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.OnNavigationReadyCallback;

public class OnNavigationReadyIdlingResource implements IdlingResource, OnNavigationReadyCallback {

  private NavigationView navigationView;
  private ResourceCallback resourceCallback;
  private boolean isNavigationReady;
  private final Handler handler = new Handler(Looper.getMainLooper());

  public OnNavigationReadyIdlingResource(Activity activity) {
    handler.post(() -> {
      navigationView = activity.findViewById(R.id.navigationView);
      navigationView.initialize(OnNavigationReadyIdlingResource.this);
    });
  }

  @Override
  public String getName() {
    return getClass().getSimpleName();
  }

  @Override
  public boolean isIdleNow() {
    return isNavigationReady;
  }

  @Override
  public void registerIdleTransitionCallback(ResourceCallback resourceCallback) {
    this.resourceCallback = resourceCallback;
  }

  public NavigationView getNavigationView() {
    return navigationView;
  }

  @Override
  public void onNavigationReady(boolean isRunning) {
    transitionToIdle();
  }

  private void transitionToIdle() {
    isNavigationReady = true;
    if (resourceCallback != null) {
      resourceCallback.onTransitionToIdle();
    }
  }
}
