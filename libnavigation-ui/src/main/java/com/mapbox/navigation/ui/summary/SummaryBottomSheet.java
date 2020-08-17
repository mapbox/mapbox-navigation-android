package com.mapbox.navigation.ui.summary;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.util.AttributeSet;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.navigation.ui.R;
import com.mapbox.navigation.base.internal.extensions.ContextEx;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.TimeFormat;
import com.mapbox.navigation.core.internal.formatter.MapboxDistanceFormatter;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.core.Rounding;
import com.mapbox.navigation.ui.NavigationViewModel;
import com.mapbox.navigation.ui.internal.utils.ViewUtils;

import java.text.DecimalFormat;
import java.util.Locale;

import static com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale;
import static com.mapbox.navigation.base.TimeFormat.NONE_SPECIFIED;

/**
 * A view with {@link com.google.android.material.bottomsheet.BottomSheetBehavior}
 * that displays route summary information during navigation.
 * <p>
 * Can be expanded / collapsed to show / hide the list of
 * directions.
 */
public class SummaryBottomSheet extends FrameLayout implements LifecycleObserver {

  private static final String EMPTY_STRING = "";
  private TextView distanceRemainingText;
  private TextView timeRemainingText;
  private TextView arrivalTimeText;
  private ProgressBar rerouteProgressBar;

  private int primaryBackgroundColor;
  private int secondaryBackgroundColor;
  private int dividerColor;
  private int progressBarBackgroundColor;
  private int primaryTextColor;
  private int secondaryTextColor;
  private int routeOverviewDrawable;

  private boolean isRerouting;
  @SuppressLint("WrongConstant")
  @TimeFormat.Type
  private int timeFormatType = NONE_SPECIFIED;
  @Nullable
  private DistanceFormatter distanceFormatter;
  private NavigationViewModel navigationViewModel;
  private LifecycleOwner lifecycleOwner;

  public SummaryBottomSheet(@NonNull Context context) {
    this(context, null);
  }

  public SummaryBottomSheet(@NonNull Context context, AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public SummaryBottomSheet(@NonNull Context context, AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initAttributes(attrs);
    initialize();
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
    applyAttributes();
  }

  /**
   * Subscribes to a {@link NavigationViewModel} for
   * updates from {@link androidx.lifecycle.LiveData}.
   * <p>
   * Updates all views with fresh data / shows &amp; hides re-route state.
   *
   * @param navigationViewModel to which this View is subscribing
   */
  public void subscribe(LifecycleOwner owner, @NonNull NavigationViewModel navigationViewModel) {
    lifecycleOwner = owner;
    lifecycleOwner.getLifecycle().addObserver(this);
    this.navigationViewModel = navigationViewModel;

    navigationViewModel.retrieveRouteProgress().observe(lifecycleOwner, this::update);
    navigationViewModel.retrieveIsOffRoute().observe(lifecycleOwner, isOffRoute -> {
      if (isOffRoute != null) {
        isRerouting = isOffRoute;
        if (isRerouting) {
          showRerouteState();
        } else {
          hideRerouteState();
        }
      }
    });
  }

  /**
   * Unsubscribes {@link NavigationViewModel} {@link androidx.lifecycle.LiveData} objects
   * previously added in {@link SummaryBottomSheet#subscribe(LifecycleOwner, NavigationViewModel)}
   * by removing the observers of the {@link LifecycleOwner} when parent view is destroyed
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  public void unsubscribe() {
    if (navigationViewModel != null) {
      navigationViewModel.retrieveRouteProgress().removeObservers(lifecycleOwner);
      navigationViewModel.retrieveIsOffRoute().removeObservers(lifecycleOwner);
    }
  }

  /**
   * Called in {@link RouteProgressObserver}, creates a new model and then
   * uses it to update the views.
   *
   * @param routeProgress used to provide navigation / routeProgress data
   */
  @SuppressWarnings("UnusedDeclaration")
  public void update(@Nullable RouteProgress routeProgress) {
    if (routeProgress != null && !isRerouting) {
      @SuppressLint("WrongConstant")
      SummaryModel model = SummaryModel.create(getContext().getApplicationContext(),
        distanceFormatter, routeProgress, timeFormatType);
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
   */
  public void showRerouteState() {
    rerouteProgressBar.setVisibility(VISIBLE);
    clearViews();
  }

  /**
   * Hides the reroute progress bar and sets
   * rerouting state to false to text will begin updating again.
   *
   */
  public void hideRerouteState() {
    rerouteProgressBar.setVisibility(INVISIBLE);
  }

  /**
   * Sets the time format type to use
   *
   * @param type to use
   */
  @SuppressLint("WrongConstant")
  public void setTimeFormat(@TimeFormat.Type int type) {
    this.timeFormatType = type;
  }

  /**
   * Sets the distance formatter
   *
   * @param distanceFormatter to set
   */
  public void setDistanceFormatter(@Nullable DistanceFormatter distanceFormatter) {
    if (distanceFormatter != null && !distanceFormatter.equals(this.distanceFormatter)) {
      this.distanceFormatter = distanceFormatter;
    }
  }

  private void initAttributes(AttributeSet attributeSet) {
    TypedArray typedArray =
        getContext().obtainStyledAttributes(attributeSet, R.styleable.MapboxStyleSummaryBottomSheet);
    primaryBackgroundColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
        R.styleable.MapboxStyleSummaryBottomSheet_summaryBottomSheetPrimaryColor,
        R.color.mapbox_summary_bottom_sheet_primary));
    secondaryBackgroundColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
        R.styleable.MapboxStyleSummaryBottomSheet_summaryBottomSheetSecondaryColor,
        R.color.mapbox_summary_bottom_sheet_secondary));
    dividerColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
        R.styleable.MapboxStyleSummaryBottomSheet_summaryBottomSheetDividerColor,
        R.color.mapbox_summary_bottom_sheet_divider));
    progressBarBackgroundColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
        R.styleable.MapboxStyleSummaryBottomSheet_summaryBottomSheetProgressBarColor,
        R.color.mapbox_summary_bottom_sheet_progress_bar));
    primaryTextColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
        R.styleable.MapboxStyleSummaryBottomSheet_summaryBottomSheetPrimaryTextColor,
        R.color.mapbox_summary_bottom_sheet_primary_text));
    secondaryTextColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
        R.styleable.MapboxStyleSummaryBottomSheet_summaryBottomSheetSecondaryTextColor,
        R.color.mapbox_summary_bottom_sheet_secondary_text));
    routeOverviewDrawable = typedArray.getResourceId(
        R.styleable.MapboxStyleSummaryBottomSheet_summaryBottomSheetRouteOverviewDrawable,
        R.drawable.mapbox_ic_route_preview);

    typedArray.recycle();
  }

  /**
   * Inflates this layout needed for this view and initializes the locale as the device locale.
   */
  private void initialize() {
    initializeDistanceFormatter();
    inflate(getContext(), R.layout.mapbox_summary_bottomsheet, this);
  }

  private void initializeDistanceFormatter() {
    final Locale locale = ContextEx.inferDeviceLocale(getContext());
    final String unitType = getUnitTypeForLocale(locale);
    distanceFormatter = new MapboxDistanceFormatter.Builder(getContext())
            .unitType(unitType)
            .roundingIncrement(Rounding.INCREMENT_FIFTY)
            .locale(locale)
            .build();
  }

  /**
   * Finds and binds all necessary views
   */
  private void bind() {
    distanceRemainingText = findViewById(R.id.distanceRemainingText);
    timeRemainingText = findViewById(R.id.timeRemainingText);
    arrivalTimeText = findViewById(R.id.arrivalTimeText);
    rerouteProgressBar = findViewById(R.id.rerouteProgressBar);
  }

  private void applyAttributes() {
    findViewById(R.id.summaryPeekLayout).setBackgroundColor(primaryBackgroundColor);
    timeRemainingText.setTextColor(primaryTextColor);
    distanceRemainingText.setTextColor(secondaryTextColor);
    arrivalTimeText.setTextColor(secondaryTextColor);

    ImageButton routeOverviewBtn = findViewById(R.id.routeOverviewBtn);
    routeOverviewBtn.setImageDrawable(AppCompatResources.getDrawable(getContext(), routeOverviewDrawable));
    Drawable routeOverviewBtnDrawable = DrawableCompat.wrap(routeOverviewBtn.getDrawable()).mutate();
    DrawableCompat.setTint(routeOverviewBtnDrawable, secondaryBackgroundColor);

    AppCompatImageButton cancelBtn = findViewById(R.id.cancelBtn);
    Drawable cancelBtnDrawable = DrawableCompat.wrap(cancelBtn.getDrawable()).mutate();
    DrawableCompat.setTint(cancelBtnDrawable, secondaryBackgroundColor);

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
      rerouteProgressBar.getIndeterminateDrawable().setTint(progressBarBackgroundColor);
    }

    if (ViewUtils.isLandscape(getContext())) {
      ImageView dividerImgViewFirst = findViewById(R.id.dividerImgViewFirst);
      Drawable dividerImgViewFirstDrawable = DrawableCompat.wrap(dividerImgViewFirst.getDrawable()).mutate();
      DrawableCompat.setTint(dividerImgViewFirstDrawable, secondaryBackgroundColor);

      ImageView dividerImgViewSecond = findViewById(R.id.dividerImgViewSecond);
      Drawable dividerImgViewSecondDrawable = DrawableCompat.wrap(dividerImgViewSecond.getDrawable()).mutate();
      DrawableCompat.setTint(dividerImgViewSecondDrawable, secondaryBackgroundColor);
    } else {
      findViewById(R.id.divider).setBackgroundColor(dividerColor);
    }
  }

  /**
   * Clears all {@link View}s.
   */
  private void clearViews() {
    arrivalTimeText.setText(EMPTY_STRING);
    timeRemainingText.setText(EMPTY_STRING);
    distanceRemainingText.setText(EMPTY_STRING);
  }
}
