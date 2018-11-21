package com.mapbox.services.android.navigation.ui.v5.summary;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.view.AsyncLayoutInflater;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.CustomLayoutUpdater;
import com.mapbox.services.android.navigation.ui.v5.NavigationBottomSheet;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewModel;
import com.mapbox.services.android.navigation.ui.v5.OnLayoutReplacedListener;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.navigation.NavigationTimeFormat;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.DistanceFormatter;
import com.mapbox.services.android.navigation.v5.utils.LocaleUtils;

/**
 * A view with {@link android.support.design.widget.BottomSheetBehavior}
 * that displays route summary information during navigation.
 * <p>
 * Can be expanded / collapsed to show / hide the list of
 * directions.
 *
 * @since 0.6.0
 */
public class SummaryBottomSheet extends FrameLayout implements NavigationBottomSheet {

  private static final String EMPTY_STRING = "";
  private TextView distanceRemainingText;
  private TextView timeRemainingText;
  private TextView arrivalTimeText;
  private ProgressBar rerouteProgressBar;
  private boolean isRerouting;
  @NavigationTimeFormat.Type
  private int timeFormatType;
  private DistanceFormatter distanceFormatter;
  private CustomLayoutUpdater layoutUpdater;

  public SummaryBottomSheet(@NonNull Context context) {
    super(context);
    initialize();
  }

  public SummaryBottomSheet(@NonNull Context context, @Nullable AttributeSet attrs) {
    super(context, attrs);
    initialize();
  }

  public SummaryBottomSheet(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initialize();
  }

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
  }

  @Override
  public void hide() {
    setVisibility(INVISIBLE);
  }

  @Override
  public void show() {
    setVisibility(VISIBLE);
  }

  /**
   * Replace this component with a pre-built {@link View}.
   *
   * @param view to be used in place of the component.
   */
  @Override
  public void replaceWith(View view) {
    CustomLayoutUpdater layoutUpdater = retrieveLayoutUpdater();
    layoutUpdater.update(this, view);
  }

  /**
   * Replace this component with a layout resource ID.  The component
   * will inflate and add the layout once it is ready.
   *
   * @param layoutResId to be inflated and added
   * @param listener    to notify when the replacement is finished
   */
  @Override
  public void replaceWith(int layoutResId, OnLayoutReplacedListener listener) {
    CustomLayoutUpdater layoutUpdater = retrieveLayoutUpdater();
    layoutUpdater.update(this, layoutResId, listener);
  }

  public void subscribe(NavigationViewModel navigationViewModel) {
    navigationViewModel.summaryModel.observe((LifecycleOwner) getContext(), new Observer<SummaryModel>() {
      @Override
      public void onChanged(@Nullable SummaryModel summaryModel) {
        if (summaryModel != null && !isRerouting) {
          arrivalTimeText.setText(summaryModel.getArrivalTime());
          timeRemainingText.setText(summaryModel.getTimeRemaining());
          distanceRemainingText.setText(summaryModel.getDistanceRemaining());
        }
      }
    });
    navigationViewModel.isOffRoute.observe((LifecycleOwner) getContext(), new Observer<Boolean>() {
      @Override
      public void onChanged(@Nullable Boolean isOffRoute) {
        if (isOffRoute != null) {
          isRerouting = isOffRoute;
          if (isRerouting) {
            showRerouteState();
          } else {
            hideRerouteState();
          }
        }
      }
    });
  }

  /**
   * Called in {@link ProgressChangeListener}, creates a new model and then
   * uses it to update the views.
   *
   * @param routeProgress used to provide navigation / routeProgress data
   * @since 0.6.2
   */
  @SuppressWarnings("UnusedDeclaration")
  public void update(RouteProgress routeProgress) {
    if (routeProgress != null && !isRerouting) {
      SummaryModel model = new SummaryModel(getContext(), distanceFormatter, routeProgress, timeFormatType);
      arrivalTimeText.setText(model.getArrivalTime());
      timeRemainingText.setText(model.getTimeRemaining());
      distanceRemainingText.setText(model.getDistanceRemaining());
    }
  }

  /**
   * Shows the reroute progress bar and clears current text views.
   * Also sets boolean to rerouting state so views do not
   * continue to update while we are fetching a new route.
   *
   * @since 0.6.0
   */
  public void showRerouteState() {
    rerouteProgressBar.setVisibility(VISIBLE);
    clearViews();
  }

  /**
   * Hides the reroute progress bar and sets
   * rerouting state to false to text will begin updating again.
   *
   * @since 0.6.0
   */
  public void hideRerouteState() {
    rerouteProgressBar.setVisibility(INVISIBLE);
  }

  /**
   * Sets the time format type to use.
   * <p>
   * This will determine how the arrival time with be formatted (12hr or 24hr).
   *
   * @param type to use
   */
  public void setTimeFormat(@NavigationTimeFormat.Type int type) {
    this.timeFormatType = type;
  }

  /**
   * Sets the distance formatter which will determine how the distance remaining
   * will be formatted, localized, and decremented.
   * <p>
   * Please see {@link DistanceFormatter} javadoc for further details.
   *
   * @param distanceFormatter to be set
   */
  public void setDistanceFormatter(DistanceFormatter distanceFormatter) {
    if (distanceFormatter != null && !distanceFormatter.equals(this.distanceFormatter)) {
      this.distanceFormatter = distanceFormatter;
    }
  }

  private void initialize() {
    initializeDistanceFormatter();
    inflate(getContext(), R.layout.summary_bottomsheet_layout, this);
  }

  private void initializeDistanceFormatter() {
    LocaleUtils localeUtils = new LocaleUtils();
    String language = localeUtils.inferDeviceLanguage(getContext());
    String unitType = localeUtils.getUnitTypeForDeviceLocale(getContext());
    int roundingIncrement = NavigationConstants.ROUNDING_INCREMENT_FIFTY;
    distanceFormatter = new DistanceFormatter(getContext(), language, unitType, roundingIncrement);
  }

  private void bind() {
    distanceRemainingText = findViewById(R.id.distanceRemainingText);
    timeRemainingText = findViewById(R.id.timeRemainingText);
    arrivalTimeText = findViewById(R.id.arrivalTimeText);
    rerouteProgressBar = findViewById(R.id.rerouteProgressBar);
    updateRouteOverviewImage();
  }

  private void updateRouteOverviewImage() {
    ImageButton routeOverviewBtn = findViewById(R.id.routeOverviewBtn);
    routeOverviewBtn.setImageDrawable(ThemeSwitcher.retrieveThemeOverviewDrawable(getContext()));
  }

  private void clearViews() {
    arrivalTimeText.setText(EMPTY_STRING);
    timeRemainingText.setText(EMPTY_STRING);
    distanceRemainingText.setText(EMPTY_STRING);
  }

  private CustomLayoutUpdater retrieveLayoutUpdater() {
    if (layoutUpdater != null) {
      return layoutUpdater;
    }
    AsyncLayoutInflater layoutInflater = new AsyncLayoutInflater(getContext());
    layoutUpdater = new CustomLayoutUpdater(layoutInflater);
    return layoutUpdater;
  }
}
