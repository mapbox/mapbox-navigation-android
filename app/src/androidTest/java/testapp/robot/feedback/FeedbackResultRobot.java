package testapp.robot.feedback;

import static android.support.test.espresso.Espresso.onView;
import static android.support.test.espresso.assertion.ViewAssertions.matches;
import static android.support.test.espresso.matcher.ViewMatchers.isDisplayed;
import static android.support.test.espresso.matcher.ViewMatchers.withText;

public class FeedbackResultRobot {
  public FeedbackResultRobot isSuccess(String successText) {
    onView(withText(successText))
      .check(matches(isDisplayed()));
    return this;
  }
}
