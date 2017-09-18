package com.mapbox.services.android.navigation.ui.v5.summary;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.location.Location;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.summary.list.DirectionListAdapter;
import com.mapbox.services.android.navigation.ui.v5.summary.list.DirectionViewHolder;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;

import java.text.DecimalFormat;

/**
 * A view with {@link android.support.design.widget.BottomSheetBehavior}
 * that displays route summary information during navigation.
 * <p>
 * Can be expanded / collapsed to show / hide the list of
 * directions.
 *
 * @since 0.6.0
 */
public class SummaryBottomSheet extends FrameLayout implements ProgressChangeListener,
  OffRouteListener {

  private static final String EMPTY_STRING = "";
  private static final int SCROLL_DIRECTION_UP = -1;

  private TextView distanceRemainingText;
  private TextView timeRemainingText;
  private TextView arrivalTimeText;
  private ProgressBar stepProgressBar;
  private ProgressBar rerouteProgressBar;
  private RecyclerView rvDirections;
  private View rvShadow;

  private DecimalFormat decimalFormat;
  private DirectionListAdapter directionListAdapter;
  private boolean rerouting;

  public SummaryBottomSheet(Context context) {
    this(context, null);
  }

  public SummaryBottomSheet(Context context, AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public SummaryBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
  }

  /**
   * After the layout inflates, binds all necessary views,
   * create a {@link RecyclerView} for the list of directions,
   * and a new {@link DecimalFormat} for formatting distance remaining.
   */
  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
    initDirectionsRecyclerView();
    initDecimalFormat();
  }

  /**
   * Listener used to update the views with navigation data.
   * <p>
   * Can be added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param location      ignored in this scenario
   * @param routeProgress holds all route / progress data needed to update the views
   * @since 0.6.0
   */
  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    update(routeProgress);
  }


  /**
   * Listener used to update the views in off-route scenario.
   * Progress bar is shown and all text is cleared.  Progress bar hides and text
   * begins populating again when the new route is received from
   * {@link com.mapbox.services.android.navigation.v5.navigation.NavigationRoute}.
   * <p>
   * Can be added to {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation}.
   *
   * @param location ignored in this scenario
   * @since 0.6.0
   */
  @Override
  public void userOffRoute(Location location) {
    showRerouteState();
  }

  /**
   * Shows the reroute progress bar and clears current text views.
   * Also sets boolean to rerouting state so views do not
   * continue to update while we are fetching a new route.
   *
   * @since 0.6.0
   */
  public void showRerouteState() {
    if (!rerouting) {
      rerouting = true;
      stepProgressBar.setVisibility(INVISIBLE);
      rerouteProgressBar.setVisibility(VISIBLE);
      clearTextViews();
    }
  }

  /**
   * Hides the reroute progress bar and sets
   * rerouting state to false to text will begin updating again.
   *
   * @since 0.6.0
   */
  public void hideRerouteState() {
    if (rerouting) {
      rerouting = false;
      stepProgressBar.setVisibility(VISIBLE);
      rerouteProgressBar.setVisibility(INVISIBLE);
    }
  }

  /**
   * Inflates this layout needed for this view.
   */
  private void init() {
    inflate(getContext(), R.layout.summary_bottomsheet_layout, this);
  }

  /**
   * Finds and binds all necessary views
   */
  private void bind() {
    distanceRemainingText = findViewById(R.id.distanceRemainingText);
    timeRemainingText = findViewById(R.id.timeRemainingText);
    arrivalTimeText = findViewById(R.id.arrivalTimeText);
    stepProgressBar = findViewById(R.id.stepProgressBar);
    rerouteProgressBar = findViewById(R.id.rerouteProgressBar);
    rvDirections = findViewById(R.id.rvDirections);
    rvShadow = findViewById(R.id.rvShadow);
  }

  /**
   * Sets up the {@link RecyclerView} that is used to display the list of directions.
   */
  private void initDirectionsRecyclerView() {
    directionListAdapter = new DirectionListAdapter();
    rvDirections.setAdapter(directionListAdapter);
    rvDirections.setHasFixedSize(true);
    rvDirections.setNestedScrollingEnabled(true);
    rvDirections.setItemAnimator(new DefaultItemAnimator());
    rvDirections.setLayoutManager(new LinearLayoutManager(getContext()));
    rvDirections.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        rvShadow.setVisibility(rvDirections.canScrollVertically(SCROLL_DIRECTION_UP) ? View.VISIBLE : View.INVISIBLE);
      }
    });
  }

  /**
   * Initializes decimal format to be used to populate views with
   * distance remaining.
   */
  private void initDecimalFormat() {
    decimalFormat = new DecimalFormat(NavigationConstants.DECIMAL_FORMAT);
  }

  /**
   * Called in {@link ProgressChangeListener}, creates a new model and then
   * uses it to update the views.
   *
   * @param routeProgress used to provide navigation / progress data
   */
  private void update(RouteProgress progress) {
    if (progress != null && !rerouting) {
      SummaryModel model = new SummaryModel(progress, decimalFormat);
      arrivalTimeText.setText(model.getArrivalTime());
      timeRemainingText.setText(model.getTimeRemaining());
      distanceRemainingText.setText(model.getDistanceRemaining());
      setProgressBar(model.getStepFractionTraveled());
      updateSteps(progress);
      updateFirstViewHolder();
    }
  }

  /**
   * Sets the current progress.
   * <p>
   * Use {@link ObjectAnimator} to provide smooth transitions between values.
   *
   * @param fractionRemaining to update progress value
   */
  private void setProgressBar(float fractionRemaining) {
    ObjectAnimator animation = ObjectAnimator.ofInt(stepProgressBar, "progress",
      Math.round(fractionRemaining * 10000));
    animation.setInterpolator(new LinearInterpolator());
    animation.setDuration(1000);
    animation.start();
  }

  /**
   * Clears all {@link TextView}s with an empty string.
   */
  private void clearTextViews() {
    arrivalTimeText.setText(EMPTY_STRING);
    timeRemainingText.setText(EMPTY_STRING);
    distanceRemainingText.setText(EMPTY_STRING);
  }

  /**
   * Used to update the directions list with the current steps.
   *
   * @param routeProgress to provide the current steps
   */
  private void updateSteps(RouteProgress routeProgress) {
    directionListAdapter.updateSteps(routeProgress);
  }

  /**
   * Sets the first {@link android.support.v7.widget.RecyclerView.ViewHolder}
   * to bold for emphasis.
   */
  private void updateFirstViewHolder() {
    if (rvDirections.findViewHolderForAdapterPosition(0) != null) {
      ((DirectionViewHolder) rvDirections.findViewHolderForAdapterPosition(0)).updateFirstViewHolder();
    }
  }
}
