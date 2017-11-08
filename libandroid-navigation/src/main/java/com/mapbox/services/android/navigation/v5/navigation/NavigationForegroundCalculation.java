package com.mapbox.services.android.navigation.v5.navigation;

import android.app.Activity;
import android.app.Application;
import android.os.Bundle;

import java.util.ArrayList;

import timber.log.Timber;

public class NavigationForegroundCalculation implements Application.ActivityLifecycleCallbacks {
  private long startSessionTime = 0;
  private ArrayList<Long> pauses = new ArrayList<Long>();
  private ArrayList<Long> resumes = new ArrayList<Long>();

  NavigationForegroundCalculation() {
    startSessionTime = System.currentTimeMillis();
  }

  @Override
  public void onActivityCreated(Activity activity, Bundle bundle) {

  }

  @Override
  public void onActivityStarted(Activity activity) {

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
  public void onActivityStopped(Activity activity) {

  }

  @Override
  public void onActivitySaveInstanceState(Activity activity, Bundle bundle) {

  }

  @Override
  public void onActivityDestroyed(Activity activity) {

  }

  public long getForegroundPercentage() {
    long currentTime = System.currentTimeMillis();
    double foregroundTime = calculateForegroundTime(currentTime);

    Timber.d("foreground - foregroundTime: " + foregroundTime);
//    Timber.d("foreground - timeSinceStart: " + (currentTime - startSessionTime));
//    Timber.d("foreground - precentage: " + percentage);
    return (long) (100 * (foregroundTime/(currentTime - startSessionTime)));
  }

  private double calculateForegroundTime(long currentTime) {
    ArrayList<Long> tempResumes = new ArrayList<>(resumes);

    if (tempResumes.size() < pauses.size()  && pauses.size() > 0) {
      tempResumes.add(currentTime);
    }

    long resumePauseDiff = 0;
    for (int i = 0; i < tempResumes.size(); i++) {
      resumePauseDiff = resumePauseDiff + (tempResumes.get(i) - pauses.get(i));
      Timber.d("foreground - resumePauseDiff: " + resumePauseDiff);
    }

    return currentTime - resumePauseDiff - startSessionTime;
  }
}
