package com.mapbox.services.android.navigation.ui.v5.summary;

import android.animation.ObjectAnimator;
import android.content.Context;
import android.location.Location;
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
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.api.directions.v5.models.DirectionsRoute;
import com.mapbox.services.api.directions.v5.models.LegStep;
import com.mapbox.services.api.directions.v5.models.RouteLeg;

import java.util.ArrayList;
import java.util.List;

public class SummaryBottomSheet extends FrameLayout implements ProgressChangeListener,
  OffRouteListener {

  private static final int SCROLL_DIRECTION_UP = -1;
  public static final String EMPTY_STRING = "";

  private TextView distanceRemainingText;
  private TextView timeRemainingText;
  private TextView arrivalTimeText;
  private ProgressBar stepProgressBar;
  private ProgressBar rerouteProgressBar;
  private RecyclerView rvDirections;
  private View rvShadow;
  private View summaryPeekLayout;

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

  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
    initDirectionsRecyclerView();
  }

  @Override
  public void onProgressChange(Location location, RouteProgress routeProgress) {
    update(routeProgress);
  }

  @Override
  public void userOffRoute(Location location) {
    showRerouteState();
  }

  public void showRerouteState() {
    if (!rerouting) {
      rerouting = true;
      stepProgressBar.setVisibility(INVISIBLE);
      rerouteProgressBar.setVisibility(VISIBLE);
      clearTextViews();
    }
  }

  public void hideRerouteState() {
    if (rerouting) {
      rerouting = false;
      stepProgressBar.setVisibility(VISIBLE);
      rerouteProgressBar.setVisibility(INVISIBLE);
    }
  }

  private void init() {
    inflate(getContext(), R.layout.summary_bottomsheet_layout, this);
  }

  private void bind() {
    distanceRemainingText = findViewById(R.id.distanceRemainingText);
    timeRemainingText = findViewById(R.id.timeRemainingText);
    arrivalTimeText = findViewById(R.id.arrivalTimeText);
    stepProgressBar = findViewById(R.id.stepProgressBar);
    rerouteProgressBar = findViewById(R.id.rerouteProgressBar);
    rvDirections = findViewById(R.id.rvDirections);
    rvShadow = findViewById(R.id.rvShadow);
    summaryPeekLayout = findViewById(R.id.summaryPeekLayout);
  }

  private void initDirectionsRecyclerView() {
    directionListAdapter = new DirectionListAdapter();
    rvDirections.setAdapter(directionListAdapter);
    rvDirections.setHasFixedSize(true);
    rvDirections.setNestedScrollingEnabled(true);
    rvDirections.setLayoutManager(new LinearLayoutManager(getContext()));
    rvDirections.addOnScrollListener(new RecyclerView.OnScrollListener() {
      @Override
      public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
        super.onScrolled(recyclerView, dx, dy);
        if (rvDirections.canScrollVertically(SCROLL_DIRECTION_UP)) {
          rvShadow.setVisibility(VISIBLE);
        } else {
          rvShadow.setVisibility(INVISIBLE);
        }
      }
    });
  }

  private void update(RouteProgress progress) {
    if (progress != null && !rerouting) {
      SummaryModel model = new SummaryModel(progress);
      arrivalTimeText.setText(model.getArrivalTime());
      timeRemainingText.setText(model.getTimeRemaining());
      distanceRemainingText.setText(model.getDistanceRemaining());
      setProgressBar(model.getStepFractionTraveled());
      addLegSteps(progress.directionsRoute());
    }
  }

  private void setProgressBar(float fractionRemaining) {
    ObjectAnimator animation = ObjectAnimator.ofInt(stepProgressBar, "progress",
      Math.round(fractionRemaining * 10000));
    animation.setInterpolator(new LinearInterpolator());
    animation.setDuration(1000);
    animation.start();
  }

  private void clearTextViews() {
    arrivalTimeText.setText(EMPTY_STRING);
    timeRemainingText.setText(EMPTY_STRING);
    distanceRemainingText.setText(EMPTY_STRING);
  }

  private void addLegSteps(DirectionsRoute directionsRoute) {
    List<LegStep> steps = new ArrayList<>();
    if (directionsRoute != null) {
      for (RouteLeg leg : directionsRoute.getLegs()) {
        steps.addAll(leg.getSteps());
      }
    }
    directionListAdapter.addLegSteps(steps);
    directionListAdapter.notifyDataSetChanged();
  }
}
