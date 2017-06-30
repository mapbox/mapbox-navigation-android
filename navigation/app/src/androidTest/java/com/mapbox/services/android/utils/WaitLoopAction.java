package com.mapbox.services.android.utils;

import android.support.test.espresso.UiController;
import android.support.test.espresso.ViewAction;
import android.view.View;

import org.hamcrest.Matcher;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class WaitLoopAction implements ViewAction {

  private long loopTime;

  public WaitLoopAction(long loopTime) {
    this.loopTime = loopTime;
  }

  @Override
  public Matcher<View> getConstraints() {
    return isDisplayed();
  }

  @Override
  public String getDescription() {
    return getClass().getSimpleName();
  }

  @Override
  public void perform(UiController uiController, View view) {
    uiController.loopMainThreadForAtLeast(loopTime);
  }

  public static void performWaitLoop(long loopTime) {
    onView(withId(android.R.id.content)).perform(new WaitLoopAction(loopTime));
  }

  public static void performWaitLoop(UiController controller, long waitTime) {
    controller.loopMainThreadForAtLeast(waitTime);
  }
}