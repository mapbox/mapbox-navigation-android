package testapp.robot.feedback;

import android.support.test.espresso.NoMatchingViewException;
import android.support.test.espresso.contrib.RecyclerViewActions;

import com.mapbox.services.android.navigation.testapp.R;

import timber.log.Timber;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.action.ViewActions.click;
import static android.support.test.espresso.matcher.ViewMatchers.withId;

public class FeedbackRobot {
  public FeedbackRobot openFeedback() {
    onView(withId(R.id.feedbackFab)).perform(click());
    return this;
  }

  public FeedbackResultRobot clickFeedbackAtPos(int pos) {
    try {
      onView(withId(R.id.feedbackItems)).perform(RecyclerViewActions.actionOnItemAtPosition(pos, click()));
    } catch (NoMatchingViewException exception) {
      Timber.e(exception.getMessage());
    }
    return new FeedbackResultRobot();
  }
}
