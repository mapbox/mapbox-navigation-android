package com.mapbox.services.android.navigation.testapp;


import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.test.espresso.ViewInteraction;
import android.support.test.rule.ActivityTestRule;
import android.support.test.rule.GrantPermissionRule;
import android.support.test.runner.AndroidJUnit4;
import android.test.suitebuilder.annotation.LargeTest;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewParent;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static android.support.test.espresso.matcher.ViewMatchers.withClassName;
import static android.support.test.espresso.matcher.ViewMatchers.withId;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.is;

@LargeTest
@RunWith(AndroidJUnit4.class)
public class WaypointNavigationActivityTest {

  private static final int ONE_SECOND = 1000;
  private static final int ONE_MINUTE = 60 * 1000;

  @Rule
  public ActivityTestRule<MainActivity> mActivityTestRule = new ActivityTestRule<>(MainActivity.class);

  @Rule
  public GrantPermissionRule mRuntimePermissionRule =
    GrantPermissionRule.grant(android.Manifest.permission.ACCESS_FINE_LOCATION);

  @Before
  public void setUp() {
    if (shouldEnableTestMode()) {
      enableTestMode();
    }
  }

  @Test
  public void waypointNavigationActivityTest() {

    sleep(ONE_SECOND);

    ViewInteraction recyclerView = onView(
      allOf(withId(R.id.recycler_view),
        childAtPosition(
          withClassName(is("android.support.constraint.ConstraintLayout")),
          0)));
    recyclerView.perform(actionOnItemAtPosition(6, click()));

    sleep(ONE_MINUTE);
  }

  private boolean shouldEnableTestMode() {
    Context context = mActivityTestRule.getActivity();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    return !preferences.getBoolean(context.getString(R.string.test_mode_key), false);
  }

  private void enableTestMode() {
    Context context = mActivityTestRule.getActivity();
    SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
    SharedPreferences.Editor editor = preferences.edit();
    editor.putBoolean(context.getString(R.string.test_mode_key), true);
    editor.apply();
  }

  private void sleep(long millis) {
    try {
      Thread.sleep(millis);
    } catch (InterruptedException e) {
      e.printStackTrace();
    }
  }

  private static Matcher<View> childAtPosition(
    final Matcher<View> parentMatcher, final int position) {

    return new TypeSafeMatcher<View>() {
      @Override
      public void describeTo(Description description) {
        description.appendText("Child at position " + position + " in parent ");
        parentMatcher.describeTo(description);
      }

      @Override
      public boolean matchesSafely(View view) {
        ViewParent parent = view.getParent();
        return parent instanceof ViewGroup && parentMatcher.matches(parent)
          && view.equals(((ViewGroup) parent).getChildAt(position));
      }
    };
  }
}
