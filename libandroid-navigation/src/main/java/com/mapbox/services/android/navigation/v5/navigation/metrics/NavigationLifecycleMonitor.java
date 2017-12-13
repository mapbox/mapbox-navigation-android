package com.mapbox.services.android.navigation.v5.navigation.metrics;

import android.app.Activity;
import android.app.Application;
import android.content.pm.ActivityInfo;
import android.os.Bundle;

import java.util.ArrayList;

public class NavigationLifecycleMonitor implements Application.ActivityLifecycleCallbacks {

  private long startSessionTime = 0;
  private ArrayList<Long> resumes;
  private ArrayList<Long> pauses;
  private ArrayList<Long> orientationChanges;
  private Integer orientation;

  public NavigationLifecycleMonitor(Application application) {
    application.registerActivityLifecycleCallbacks(this);
    startSessionTime = System.currentTimeMillis();
    resumes = new ArrayList<>();
    pauses = new ArrayList<>();
    orientationChanges = new ArrayList<>();
    orientation = application.getResources().getConfiguration().orientation;
  }

  @Override
  public void onActivityStarted(Activity activity) {
    int newOrientation = activity.getResources().getConfiguration().orientation;
    // If not equal, add orientation change time
    if (orientation != newOrientation) {
      orientation = newOrientation;
      orientationChanges.add(System.currentTimeMillis());
    }
  }

  @Override
  public void onActivityResumed(Activity activity) {
    resumes.add(System.currentTimeMillis());
  }

  @Override
  public void onActivityPaused(Activity activity) {
    pauses.add(System.currentTimeMillis());
  }

  @Override
  public void onActivityDestroyed(Activity activity) {
    if (activity.isFinishing()) {
      activity.getApplication().unregisterActivityLifecycleCallbacks(this);
    }
  }

  //region Unused Lifecycle Methods

  @Override
  public void onActivityCreated(Activity activity, Bundle bundle) {

  }

  @Override
  public void onActivityStopped(Activity activity) {

  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

  }

  //endregion

  public int obtainPortraitPercentage() {
    return calculatePortraitPercentage();
  }

  private int calculatePortraitPercentage() {
    boolean isPortrait = orientation.equals(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
    // If there were no orientation changes and currently in portrait, return 100
    // Otherwise, return 0 (landscape the whole time)
    if (orientationChanges.isEmpty()) {
      return isPortrait ? 100 : 0;
    }

    // Find the time spent in portrait
    long portraitTimeInMillis = 0;
    long lastChangeTimeInMillis = 0;
    long currentTimeMillis = System.currentTimeMillis();
    for (int i = orientationChanges.size() - 1; i > 0; i--) {
      if (isPortrait) {
        long diffFromLastChange;
        // First iteration of the loop
        if (lastChangeTimeInMillis == 0) {
          diffFromLastChange = currentTimeMillis - orientationChanges.get(i);
        } else {
          diffFromLastChange = lastChangeTimeInMillis - orientationChanges.get(i);
        }
        lastChangeTimeInMillis = orientationChanges.get(i);
        portraitTimeInMillis = portraitTimeInMillis + diffFromLastChange;
        // Next change time in array is landscape
        isPortrait = false;
      } else {
        lastChangeTimeInMillis = orientationChanges.get(i);
        // Next change time in array is portrait
        isPortrait = true;
      }
    }
    return (int) (100 * (portraitTimeInMillis / (currentTimeMillis - startSessionTime)));
  }

  public int obtainForegroundPercentage() {
    long currentTime = System.currentTimeMillis();
    double foregroundTime = calculateForegroundTime(currentTime);
    return (int) (100 * (foregroundTime / (currentTime - startSessionTime)));
  }

  private double calculateForegroundTime(long currentTime) {
    ArrayList<Long> tempResumes = new ArrayList<>(resumes);

    // If the activity was destroyed while in the background
    if (tempResumes.size() < pauses.size() && pauses.size() > 0) {
      tempResumes.add(currentTime);
    }
    long resumePauseDiff = 0;
    for (int i = 0; i < tempResumes.size(); i++) {
      resumePauseDiff = resumePauseDiff + (tempResumes.get(i) - pauses.get(i));
    }
    return currentTime - resumePauseDiff - startSessionTime;
  }
}
