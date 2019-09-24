package testapp.action;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;

import com.mapbox.services.android.navigation.ui.v5.NavigationView;

import org.hamcrest.Matcher;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

public class NavigationViewAction implements ViewAction {

  private OnInvokeActionListener invokeViewAction;
  private NavigationView navigationView;

  private NavigationViewAction(OnInvokeActionListener invokeViewAction, NavigationView navigationView) {
    this.invokeViewAction = invokeViewAction;
    this.navigationView = navigationView;
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
    invokeViewAction.onInvokeAction(uiController, navigationView);
  }

  public static void invoke(NavigationView navigationView, OnInvokeActionListener invokeViewAction) {
    onView(withId(android.R.id.content)).perform(new NavigationViewAction(invokeViewAction, navigationView));
  }

  public interface OnInvokeActionListener {
    void onInvokeAction(@NonNull UiController uiController, @NonNull NavigationView navigationView);
  }
}
