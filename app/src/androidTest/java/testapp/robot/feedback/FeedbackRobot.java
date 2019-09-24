package testapp.robot.feedback;


import com.mapbox.services.android.navigation.testapp.R;
import androidx.test.espresso.contrib.RecyclerViewActions;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class FeedbackRobot {
  public FeedbackRobot openFeedback() {
    onView(withId(R.id.feedbackFab)).perform(click());
    return this;
  }

  public FeedbackResultRobot clickFeedbackAtPos(int pos) {
    onView(withId(R.id.feedbackItems)).perform(RecyclerViewActions.actionOnItemAtPosition(pos, click()));
    return new FeedbackResultRobot();
  }
}
