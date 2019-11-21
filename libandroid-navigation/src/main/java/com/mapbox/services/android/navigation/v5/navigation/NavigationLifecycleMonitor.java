package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Activity;
import android.app.Application;
import android.content.res.Configuration;
import android.os.Bundle;

import java.util.ArrayList;

public class NavigationLifecycleMonitor implements Application.ActivityLifecycleCallbacks {

  private static final int ONE_HUNDRED_PERCENT = 100;

  private long startSessionTime = 0;
  private ArrayList<Long> resumes;
  private ArrayList<Long> pauses;
  private Integer currentOrientation;
  private long portraitStartTime = 0;
  private double portraitTimeInMillis = 0;

  NavigationLifecycleMonitor(Application application) {
    application.registerActivityLifecycleCallbacks(this);
    startSessionTime = System.currentTimeMillis();
    resumes = new ArrayList<>();
    pauses = new ArrayList<>();
    initCurrentOrientation(application);
  }

  @Override
  public void onActivityStarted(Activity activity) {
    int newOrientation = activity.getResources().getConfiguration().orientation;
    // If a new orientation is found, set it to the current
    if (!currentOrientation.equals(newOrientation)) {
      currentOrientation = newOrientation;
      long currentTimeMillis = System.currentTimeMillis();
      // If the current orientation is now landscape, add the time the phone was just in portrait
      if (currentOrientation.equals(Configuration.ORIENTATION_LANDSCAPE)) {
        portraitTimeInMillis = portraitTimeInMillis + (currentTimeMillis - portraitStartTime);
      } else if (currentOrientation.equals(Configuration.ORIENTATION_PORTRAIT)) {
        portraitStartTime = currentTimeMillis;
      }
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

  int obtainPortraitPercentage() {
    // If no changes to landscape
    if (currentOrientation.equals(Configuration.ORIENTATION_PORTRAIT) && portraitTimeInMillis == 0) {
      return ONE_HUNDRED_PERCENT;
    }
    // Calculate given the time spent in portrait
    double portraitFraction = portraitTimeInMillis / (System.currentTimeMillis() - startSessionTime);
    return (int) (ONE_HUNDRED_PERCENT * portraitFraction);
  }

  int obtainForegroundPercentage() {
    long currentTime = System.currentTimeMillis();
    double foregroundTime = calculateForegroundTime(currentTime);
    return (int) (100 * (foregroundTime / (currentTime - startSessionTime)));
  }

  private void initCurrentOrientation(Application application) {
    currentOrientation = application.getResources().getConfiguration().orientation;
    // If starting in portrait, set the portrait start time
    if (currentOrientation.equals(Configuration.ORIENTATION_PORTRAIT)) {
      portraitStartTime = System.currentTimeMillis();
    }
  }

  private double calculateForegroundTime(long currentTime) {
    ArrayList<Long> tempResumes = new ArrayList<>(resumes);

    // If the activity was destroyed while in the background
    if (tempResumes.size() < pauses.size() && pauses.size() > 0) {
      tempResumes.add(currentTime);
    }
    long resumePauseDiff = 0;
    for (int i = 0; i < tempResumes.size(); i++) {
      if (i < pauses.size()) {
        resumePauseDiff = resumePauseDiff + (tempResumes.get(i) - pauses.get(i));
      }
    }
    return currentTime - resumePauseDiff - startSessionTime;
  }
}
