package testapp.robot.feedback;


import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

public class FeedbackResultRobot {
  public FeedbackResultRobot isSuccess(String successText) {
    onView(withText(successText))
      .check(matches(isDisplayed()));
    return this;
  }
}
