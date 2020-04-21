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

import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.AppCompatImageButton;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.Observer;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.RecyclerView;

import com.mapbox.libnavigation.ui.R;
import com.mapbox.navigation.base.internal.extensions.ContextEx;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.base.typedef.RoundingIncrementKt;
import com.mapbox.navigation.base.typedef.TimeFormatType;
import com.mapbox.navigation.core.MapboxDistanceFormatter;
import com.mapbox.navigation.base.trip.RouteProgressObserver;
import com.mapbox.navigation.ui.NavigationViewModel;
import com.mapbox.navigation.ui.utils.ViewUtils;

import java.text.DecimalFormat;
import java.util.Locale;

import static com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale;
import static com.mapbox.navigation.base.typedef.TimeFormatTypeKt.NONE_SPECIFIED;

/**
 * A view with {@link com.google.android.material.bottomsheet.BottomSheetBehavior}
 * that displays route summary information during navigation.
 * <p>
 * Can be expanded / collapsed to show / hide the list of
 * directions.
 *
 * @since 0.6.0
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
  @TimeFormatType
  private int timeFormatType = NONE_SPECIFIED;
  private DistanceFormatter distanceFormatter;
  private NavigationViewModel navigationViewModel;
  private LifecycleOwner lifecycleOwner;

  public SummaryBottomSheet(Context context) {
    this(context, null);
  }

  public SummaryBottomSheet(Context context, AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public SummaryBottomSheet(Context context, AttributeSet attrs, int defStyleAttr) {
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
   * @since 0.6.2
   */
  public void subscribe(LifecycleOwner owner, NavigationViewModel navigationViewModel) {
    lifecycleOwner = owner;
    lifecycleOwner.getLifecycle().addObserver(this);
    this.navigationViewModel = navigationViewModel;

    navigationViewModel.retrieveSummaryModel().observe(lifecycleOwner, new Observer<SummaryModel>() {
      @Override
      public void onChanged(@Nullable SummaryModel summaryModel) {
        if (summaryModel != null && !isRerouting) {
          arrivalTimeText.setText(summaryModel.getArrivalTime());
          timeRemainingText.setText(summaryModel.getTimeRemaining());
          distanceRemainingText.setText(summaryModel.getDistanceRemaining());
        }
      }
    });
    navigationViewModel.retrieveIsOffRoute().observe(lifecycleOwner, new Observer<Boolean>() {
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
   * Unsubscribes {@link NavigationViewModel} {@link androidx.lifecycle.LiveData} objects
   * previously added in {@link SummaryBottomSheet#subscribe(LifecycleOwner, NavigationViewModel)}
   * by removing the observers of the {@link LifecycleOwner} when parent view is destroyed
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  public void unsubscribe() {
    if (navigationViewModel != null) {
      navigationViewModel.retrieveSummaryModel().removeObservers(lifecycleOwner);
      navigationViewModel.retrieveIsOffRoute().removeObservers(lifecycleOwner);
    }
  }

  /**
   * Called in {@link RouteProgressObserver}, creates a new model and then
   * uses it to update the views.
   *
   * @param routeProgress used to provide navigation / routeProgress data
   * @since 0.6.2
   */
  @SuppressWarnings("UnusedDeclaration")
  public void update(RouteProgress routeProgress) {
    if (routeProgress != null && !isRerouting) {
      @SuppressLint("WrongConstant")
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
   * Sets the time format type to use
   *
   * @param type to use
   */
  @SuppressLint("WrongConstant")
  public void setTimeFormat(@TimeFormatType int type) {
    this.timeFormatType = type;
  }

  /**
   * Sets the distance formatter
   *
   * @param distanceFormatter to set
   */
  public void setDistanceFormatter(DistanceFormatter distanceFormatter) {
    if (distanceFormatter != null && !distanceFormatter.equals(this.distanceFormatter)) {
      this.distanceFormatter = distanceFormatter;
    }
  }

  private void initAttributes(AttributeSet attributeSet) {
    TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.SummaryBottomSheet);
    primaryBackgroundColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
      R.styleable.SummaryBottomSheet_summaryBottomSheetPrimaryColor, R.color.mapbox_summary_bottom_sheet_primary));
    secondaryBackgroundColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
      R.styleable.SummaryBottomSheet_summaryBottomSheetSecondaryColor, R.color.mapbox_summary_bottom_sheet_secondary));
    dividerColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
      R.styleable.SummaryBottomSheet_summaryBottomSheetDividerColor, R.color.mapbox_summary_bottom_sheet_divider));
    progressBarBackgroundColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
      R.styleable.SummaryBottomSheet_summaryBottomSheetProgressBarColor,
      R.color.mapbox_summary_bottom_sheet_progress_bar));
    primaryTextColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
      R.styleable.SummaryBottomSheet_summaryBottomSheetPrimaryTextColor,
      R.color.mapbox_summary_bottom_sheet_primary_text));
    secondaryTextColor = ContextCompat.getColor(getContext(), typedArray.getResourceId(
      R.styleable.SummaryBottomSheet_summaryBottomSheetSecondaryTextColor,
      R.color.mapbox_summary_bottom_sheet_secondary_text));
    routeOverviewDrawable = typedArray.getResourceId(
      R.styleable.SummaryBottomSheet_summaryBottomSheetRouteOverviewDrawable, R.drawable.ic_route_preview);

    typedArray.recycle();
  }

  /**
   * Inflates this layout needed for this view and initializes the locale as the device locale.
   */
  private void initialize() {
    initializeDistanceFormatter();
    inflate(getContext(), R.layout.summary_bottomsheet_layout, this);
  }

  private void initializeDistanceFormatter() {
    final Locale locale = ContextEx.inferDeviceLocale(getContext());
    final String unitType = getUnitTypeForLocale(locale);
    distanceFormatter = new MapboxDistanceFormatter.Builder(getContext())
            .withUnitType(unitType)
            .withRoundingIncrement(RoundingIncrementKt.ROUNDING_INCREMENT_FIFTY)
            .withLocale(locale)
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
