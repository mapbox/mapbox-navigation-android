package com.mapbox.navigation.ui.instruction;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.ColorRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.constraintlayout.widget.ConstraintSet;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.LifecycleOwner;
import androidx.lifecycle.OnLifecycleEvent;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.AutoTransition;
import androidx.transition.TransitionManager;

import com.mapbox.api.directions.v5.models.BannerComponents;
import com.mapbox.api.directions.v5.models.BannerInstructions;
import com.mapbox.api.directions.v5.models.BannerText;
import com.mapbox.api.directions.v5.models.LegStep;
import com.mapbox.api.directions.v5.models.ManeuverModifier;
import com.mapbox.libnavigation.ui.R;
import com.mapbox.navigation.base.formatter.DistanceFormatter;
import com.mapbox.navigation.base.internal.extensions.ContextEx;
import com.mapbox.navigation.base.trip.model.RouteProgress;
import com.mapbox.navigation.core.MapboxNavigation;
import com.mapbox.navigation.core.Rounding;
import com.mapbox.navigation.core.internal.MapboxDistanceFormatter;
import com.mapbox.navigation.core.trip.session.BannerInstructionsObserver;
import com.mapbox.navigation.core.trip.session.OffRouteObserver;
import com.mapbox.navigation.core.trip.session.RouteProgressObserver;
import com.mapbox.navigation.ui.FeedbackButton;
import com.mapbox.navigation.ui.NavigationButton;
import com.mapbox.navigation.ui.NavigationViewModel;
import com.mapbox.navigation.ui.SoundButton;
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheet;
import com.mapbox.navigation.ui.feedback.FeedbackBottomSheetListener;
import com.mapbox.navigation.ui.feedback.FeedbackItem;
import com.mapbox.navigation.ui.instruction.maneuver.ManeuverView;
import com.mapbox.navigation.ui.internal.instruction.InstructionModel;
import com.mapbox.navigation.ui.internal.instruction.turnlane.TurnLaneAdapter;
import com.mapbox.navigation.ui.internal.summary.InstructionListAdapter;
import com.mapbox.navigation.ui.internal.utils.ViewUtils;
import com.mapbox.navigation.ui.listeners.InstructionListListener;

import org.jetbrains.annotations.NotNull;

import java.util.Locale;

import timber.log.Timber;

import static com.mapbox.navigation.base.internal.extensions.LocaleEx.getUnitTypeForLocale;
import static com.mapbox.navigation.ui.NavigationConstants.FEEDBACK_BOTTOM_SHEET_DURATION;

/**
 * A view that can be used to display upcoming maneuver information and control
 * voice instruction mute / unmute.
 * <p>
 * An {@link ImageView} is used to display the maneuver image on the left.
 * Two {@link TextView}s are used to display distance to the next maneuver, as well
 * as the name of the destination / maneuver name / instruction based on what data is available
 * <p>
 * To automatically have this view update with information from
 * {@link MapboxNavigation},
 * add the view as a {@link RouteProgressObserver} and / or {@link OffRouteObserver}
 */
public class InstructionView extends RelativeLayout implements LifecycleObserver, FeedbackBottomSheetListener {

  private static final String COMPONENT_TYPE_LANE = "lane";
  private static final long GUIDANCE_VIEW_TRANSITION_SPEED = 900L;
  private static final double MAXIMUM_PRIMARY_INSTRUCTION_TEXT_WIDTH_RATIO_IN_LANDSCAPE = 0.75;

  private ManeuverView maneuverView;
  private TextView stepDistanceText;
  private TextView stepPrimaryText;
  private TextView stepSecondaryText;
  private ManeuverView subManeuverView;
  private TextView subStepText;
  private NavigationAlertView alertView;
  private View rerouteLayout;
  private TextView rerouteText;
  private View turnLaneLayout;
  private View subStepLayout;
  private ImageView guidanceViewImage;
  private RecyclerView rvTurnLanes;
  private RecyclerView rvInstructions;
  private TurnLaneAdapter turnLaneAdapter;
  private ConstraintLayout instructionLayout;
  private LinearLayout instructionLayoutText;
  private View instructionListLayout;
  private InstructionListAdapter instructionListAdapter;
  private Animation rerouteSlideUpTop;
  private Animation rerouteSlideDownTop;
  private LegStep currentStep;
  private NavigationViewModel navigationViewModel;
  private InstructionListListener instructionListListener;

  private DistanceFormatter distanceFormatter;
  private boolean isRerouting;
  private SoundButton soundButton;
  private FeedbackButton feedbackButton;
  private LifecycleOwner lifecycleOwner;
  private GuidanceViewImageProvider guidanceViewImageProvider = new GuidanceViewImageProvider();
  private GuidanceViewImageProvider.OnGuidanceImageDownload callback =
    new GuidanceViewImageProvider.OnGuidanceImageDownload() {
    @Override
    public void onGuidanceImageReady(@NotNull Bitmap bitmap) {
      animateShowGuidanceViewImage();
      guidanceViewImage.setImageBitmap(bitmap);
    }

    @Override
    public void onNoGuidanceImageUrl() {
      animateHideGuidanceViewImage();
    }

    @Override
    public void onFailure(@org.jetbrains.annotations.Nullable String message) {
      animateHideGuidanceViewImage();
    }
  };

  private int primaryBackgroundColor;
  private int secondaryBackgroundColor;
  private int listViewBackgroundColor;
  private int primaryTextColor;
  private int secondaryTextColor;
  private int maneuverViewPrimaryColor;
  private int maneuverViewSecondaryColor;
  private int maneuverViewStyle;
  private int soundButtonStyle;
  private int feedbackButtonStyle;
  private int alertViewStyle;

  public InstructionView(Context context) {
    this(context, null);
  }

  public InstructionView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public InstructionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    initAttributes(attrs);
    initialize();
  }

  /**
   * Adds a listener that is triggered when the instruction list in InstructionView is shown or hidden.
   *
   * @param instructionListListener to be set
   */
  public void setInstructionListListener(InstructionListListener instructionListListener) {
    this.instructionListListener = instructionListListener;
  }

  @Override
  public void onFeedbackSelected(FeedbackItem feedbackItem) {
    navigationViewModel.updateFeedback(feedbackItem);
  }

  @Override
  public void onFeedbackDismissed() {
  }

  /**
   * Subscribes to a {@link NavigationViewModel} for
   * updates from {@link androidx.lifecycle.LiveData}.
   * <p>
   * Updates all views with fresh data / shows &amp; hides re-route state.
   *
   * @param navigationViewModel to which this View is subscribing
   */
  public void subscribe(LifecycleOwner owner, NavigationViewModel navigationViewModel) {
    lifecycleOwner = owner;
    lifecycleOwner.getLifecycle().addObserver(this);
    this.navigationViewModel = navigationViewModel;

    navigationViewModel.retrieveBannerInstructions().observe(lifecycleOwner, bannerInstructions -> {
      toggleGuidanceView(bannerInstructions);
      updateBannerInstructionsWith(bannerInstructions);
    });

    navigationViewModel.retrieveRouteProgress().observe(lifecycleOwner, routeProgress -> {
      if (routeProgress != null) {
        updateDistanceWith(routeProgress);
      }
    });

    navigationViewModel.retrieveIsOffRoute().observe(lifecycleOwner, isOffRoute -> {
      if (isOffRoute != null) {
        if (isOffRoute) {
          showRerouteState();
        } else if (isRerouting) {
          hideRerouteState();
          alertView.showReportProblem();
        }
        isRerouting = isOffRoute;
      }
    });

    subscribeAlertView();
    initializeButtonListeners();
    showButtons();
  }

  /**
   * Unsubscribes {@link NavigationViewModel} {@link androidx.lifecycle.LiveData} objects
   * previously added in {@link InstructionView#subscribe(LifecycleOwner, NavigationViewModel)}
   * by removing the observers of the {@link LifecycleOwner} when parent view is destroyed
   */
  @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
  public void unsubscribe() {
    if (navigationViewModel != null) {
      navigationViewModel.retrieveRouteProgress().removeObservers(lifecycleOwner);
      navigationViewModel.retrieveBannerInstructions().removeObservers(lifecycleOwner);
      navigationViewModel.retrieveIsOffRoute().removeObservers(lifecycleOwner);
    }
    ImageCreator.getInstance().shutdown();
  }

  /**
   * Use this method inside a {@link RouteProgressObserver} to update this view.
   * <p>
   * This includes the distance remaining, instruction list, turn lanes, and next step information.
   *
   * @param routeProgress for route data used to populate the views
   */
  public void updateDistanceWith(RouteProgress routeProgress) {
    if (routeProgress != null && !isRerouting) {
      InstructionModel model = new InstructionModel(distanceFormatter, routeProgress);
      updateDataFromInstruction(model);
    }
  }

  /**
   * Use this method inside a {@link BannerInstructionsObserver} to update banner instruction view.
   * <p>
   * This includes the maneuverView, banner text and sub step information.
   * A default {@link ManeuverModifier#RIGHT} driving side will be used if driving side is unavailable.
   *
   * @param instructions for banner info used to populate the views
   */
  public void updateBannerInstructionsWith(BannerInstructions instructions) {
    updateBannerInstructionsWith(instructions, ManeuverModifier.RIGHT);
  }

  /**
   * Use this method inside a {@link BannerInstructionsObserver} to update banner instruction view.
   * <p>
   * This includes the maneuverView, banner text and sub step information.
   * The drivingSideFallback will be used when driving side is unavailable.
   *
   * @param instructions for banner info used to populate the views
   * @param drivingSideFallback one of {@link ManeuverModifier#RIGHT} or {@link ManeuverModifier#LEFT}.
   */
  public void updateBannerInstructionsWith(BannerInstructions instructions, String drivingSideFallback) {
    if (instructions != null) {
      updateBannerInstructions(instructions.primary(),
          instructions.secondary(), instructions.sub(), getDrivingSide(drivingSideFallback));
    }
  }

  /**
   * Shows {@link FeedbackBottomSheet} and adds a listener so
   * the proper feedback information is collected or the user dismisses the UI.
   */
  public void showFeedbackBottomSheet() {
    FragmentManager fragmentManager = obtainSupportFragmentManager();
    if (fragmentManager != null) {
      FeedbackBottomSheet.newInstance(this, FEEDBACK_BOTTOM_SHEET_DURATION)
          .show(fragmentManager, FeedbackBottomSheet.TAG);
    }
  }

  /**
   * Will slide the reroute view down from the top of the screen
   * and make it visible
   */
  public void showRerouteState() {
    if (rerouteLayout.getVisibility() == INVISIBLE) {
      rerouteLayout.startAnimation(rerouteSlideDownTop);
      rerouteLayout.setVisibility(VISIBLE);
    }
  }

  /**
   * Will slide the reroute view up to the top of the screen
   * and hide it
   */
  public void hideRerouteState() {
    if (rerouteLayout.getVisibility() == VISIBLE) {
      rerouteLayout.startAnimation(rerouteSlideUpTop);
      rerouteLayout.setVisibility(INVISIBLE);
    }
  }

  /**
   * Can be used to determine the visibility of the instruction list.
   *
   * @return true if instruction list is visible, false is not
   */
  public boolean isShowingInstructionList() {
    return instructionListLayout.getVisibility() == VISIBLE;
  }

  /**
   * Hide the instruction list.
   * <p>
   * This is based on orientation so the different layouts (for portrait vs. landscape)
   * can be animated appropriately.
   */
  public void hideInstructionList() {
    rvInstructions.stopScroll();
    beginDelayedTransition();
    if (ViewUtils.isLandscape(getContext())) {
      updateLandscapeConstraintsTo(R.layout.instruction_layout);
      rerouteLayout.setBackgroundColor(primaryBackgroundColor);
    }
    instructionListLayout.setVisibility(GONE);
    onInstructionListVisibilityChanged(false);
  }

  /**
   * Show the instruction list.
   * <p>
   * This is based on orientation so the different layouts (for portrait vs. landscape)
   * can be animated appropriately.
   */
  public void showInstructionList() {
    onInstructionListVisibilityChanged(true);
    instructionLayout.requestFocus();
    if (ViewUtils.isLandscape(getContext())) {
      updateLandscapeConstraintsTo(R.layout.instruction_layout_alt);
      rerouteLayout.setBackgroundColor(secondaryBackgroundColor);
    }

    final Animation animation = AnimationUtils.loadAnimation(this.getContext(), R.anim.instruction_view_fade_in);
    animation.setAnimationListener(getInstructionListAnimationListener());
    instructionListLayout.setVisibility(VISIBLE);
    instructionListLayout.startAnimation(animation);
  }

  public boolean handleBackPressed() {
    if (isShowingInstructionList()) {
      hideInstructionList();
      return true;
    }
    return false;
  }

  /**
   * Sets the distance formatter
   *
   * @param distanceFormatter to set
   */
  public void setDistanceFormatter(DistanceFormatter distanceFormatter) {
    if (distanceFormatter != null && !distanceFormatter.equals(this.distanceFormatter)) {
      this.distanceFormatter = distanceFormatter;
      instructionListAdapter.updateDistanceFormatter(distanceFormatter);
    }
  }

  /**
   * Gets the sound button which is used for muting/unmuting, for uses such as adding listeners and
   * hiding the button.
   *
   * @return sound button with {@link NavigationButton} API
   */
  public NavigationButton retrieveSoundButton() {
    return soundButton;
  }

  /**
   * Gets the feedback button which is used for sending feedback, for uses such as adding listeners
   * and hiding the button.
   *
   * @return feedback button with {@link NavigationButton} API
   */
  public NavigationButton retrieveFeedbackButton() {
    return feedbackButton;
  }

  /**
   * Returns the {@link NavigationAlertView} that is shown during off-route events with
   * "Report a Problem" text.
   *
   * @return alert view that is used in the instruction view
   */
  public NavigationAlertView retrieveAlertView() {
    return alertView;
  }

  /**
   * The method determines if a junction is arriving and displays appropriate junction view image based
   * on the distance remaining for the junction to arrive.
   *
   * @param bannerInstructions {@link BannerInstructions}
   */
  public void toggleGuidanceView(@Nullable BannerInstructions bannerInstructions) {
    if (bannerInstructions != null && guidanceViewImageProvider != null) {
      guidanceViewImageProvider.renderGuidanceView(bannerInstructions, getContext(), callback);
    } else {
      animateHideGuidanceViewImage();
    }
  }

  /**
   * Once this view has finished inflating, it will bind the views.
   * <p>
   * It will also initialize the {@link RecyclerView} used to display the turn lanes
   * and animations used to show / hide views.
   */
  @Override
  protected void onFinishInflate() {
    super.onFinishInflate();
    bind();
    applyAttributes();
    initializeTurnLaneRecyclerView();
    initializeInstructionListRecyclerView();
    initializeAnimations();
    initializeStepListClickListener();
    initializeButtons();
    ImageCreator.getInstance().initialize(getContext());
  }

  @Override
  protected void onAttachedToWindow() {
    super.onAttachedToWindow();
    addBottomSheetListener();
  }

  @Override
  protected void onDetachedFromWindow() {
    super.onDetachedFromWindow();
    cancelDelayedTransition();
  }

  private void initAttributes(AttributeSet attributeSet) {
    TypedArray typedArray = getContext().obtainStyledAttributes(attributeSet, R.styleable.InstructionView);
    primaryBackgroundColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(
            R.styleable.InstructionView_instructionViewPrimaryBackgroundColor,
            R.color.mapbox_instruction_view_primary_background));

    secondaryBackgroundColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(
            R.styleable.InstructionView_instructionViewSecondaryBackgroundColor,
            R.color.mapbox_instruction_view_secondary_background));

    listViewBackgroundColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(
            R.styleable.InstructionView_instructionListViewBackgroundColor,
            R.color.mapbox_instruction_list_view_background));

    primaryTextColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(
            R.styleable.InstructionView_instructionViewPrimaryTextColor,
            R.color.mapbox_instruction_view_primary_text));

    secondaryTextColor = ContextCompat.getColor(getContext(),
        typedArray.getResourceId(
            R.styleable.InstructionView_instructionViewSecondaryTextColor,
            R.color.mapbox_instruction_view_secondary_text));

    maneuverViewStyle = typedArray.getResourceId(
        R.styleable.InstructionView_instructionViewManeuverViewStyle, R.style.ManeuverView);
    soundButtonStyle = typedArray.getResourceId(
        R.styleable.InstructionView_instructionViewSoundButtonStyle, -1);
    feedbackButtonStyle = typedArray.getResourceId(
        R.styleable.InstructionView_instructionViewFeedbackButtonStyle, -1);
    alertViewStyle = typedArray.getResourceId(
        R.styleable.InstructionView_instructionViewAlertViewStyle, -1);

    typedArray.recycle();

    initManeuverViewAttributes();
  }

  @SuppressLint("CustomViewStyleable")
  private void initManeuverViewAttributes() {
    TypedArray maneuverViewTypedArray =
        getContext().obtainStyledAttributes(maneuverViewStyle, R.styleable.ManeuverView);
    @ColorRes int maneuverViewPrimaryColorRes = maneuverViewTypedArray.getResourceId(
        R.styleable.ManeuverView_maneuverViewPrimaryColor,
        R.color.mapbox_instruction_maneuver_view_primary);
    maneuverViewPrimaryColor = ContextCompat.getColor(getContext(), maneuverViewPrimaryColorRes);

    @ColorRes int maneuverViewSecondaryColorRes = maneuverViewTypedArray.getResourceId(
        R.styleable.ManeuverView_maneuverViewSecondaryColor,
        R.color.mapbox_instruction_maneuver_view_secondary);
    maneuverViewSecondaryColor = ContextCompat.getColor(getContext(), maneuverViewSecondaryColorRes);

    if (maneuverViewPrimaryColorRes == R.color.mapbox_instruction_maneuver_view_primary) {
      // Ref: https://github.com/mapbox/mapbox-navigation-android/issues/3133
      // if using default MapboxManeuverView, need to assign MapboxManeuverView style for turnLane view use.
      maneuverViewStyle = R.style.MapboxManeuverView;
    }

    maneuverViewTypedArray.recycle();
  }

  /**
   * Inflates this layout needed for this view and initializes the locale as the device locale.
   */
  private void initialize() {
    final String unitType = getUnitTypeForLocale(ContextEx.inferDeviceLocale(getContext()));
    final int roundingIncrement = Rounding.INCREMENT_FIFTY;
    final Locale locale = ContextEx.inferDeviceLocale(getContext());
    distanceFormatter = new MapboxDistanceFormatter.Builder()
        .withUnitType(unitType)
        .withRoundingIncrement(roundingIncrement)
        .withLocale(locale)
        .build(getContext());
    inflate(getContext(), R.layout.instruction_view_layout, this);
  }

  /**
   * Finds and binds all necessary views
   */
  private void bind() {
    instructionLayout = findViewById(R.id.instructionLayout);
    instructionLayoutText = findViewById(R.id.instructionLayoutText);

    maneuverView = findViewById(R.id.maneuverView);
    stepPrimaryText = findViewById(R.id.stepPrimaryText);
    stepDistanceText = findViewById(R.id.stepDistanceText);
    stepSecondaryText = findViewById(R.id.stepSecondaryText);

    subStepLayout = findViewById(R.id.subStepLayout);
    subManeuverView = findViewById(R.id.subManeuverView);
    subStepText = findViewById(R.id.subStepText);

    rerouteLayout = findViewById(R.id.rerouteLayout);
    rerouteText = findViewById(R.id.rerouteText);

    guidanceViewImage = findViewById(R.id.guidanceImageView);

    turnLaneLayout = findViewById(R.id.turnLaneLayout);
    rvTurnLanes = findViewById(R.id.rvTurnLanes);
    rvInstructions = findViewById(R.id.rvInstructions);

    instructionListLayout = findViewById(R.id.instructionListLayout);
    alertView = findViewById(R.id.alertView);
    soundButton = findViewById(R.id.soundLayout);
    feedbackButton = findViewById(R.id.feedbackLayout);
  }

  /**
   * Use customized attributes to update view colors
   */
  private void applyAttributes() {
    if (ViewUtils.isLandscape(getContext())) {
      applyAttributesForLandscape();
    } else {
      applyAttributesForPortrait();
    }

    instructionListLayout.setBackgroundColor(listViewBackgroundColor);

    maneuverView.setPrimaryColor(maneuverViewPrimaryColor);
    maneuverView.setSecondaryColor(maneuverViewSecondaryColor);
    stepPrimaryText.setTextColor(primaryTextColor);
    stepDistanceText.setTextColor(secondaryTextColor);
    stepSecondaryText.setTextColor(secondaryTextColor);

    subManeuverView.setPrimaryColor(maneuverViewPrimaryColor);
    subManeuverView.setSecondaryColor(maneuverViewSecondaryColor);
    subStepText.setTextColor(secondaryTextColor);

    rerouteLayout.setBackgroundColor(primaryBackgroundColor);
    rerouteText.setTextColor(primaryTextColor);

    if (soundButtonStyle != -1) {
      soundButton.updateStyle(soundButtonStyle);
    }
    if (feedbackButtonStyle != -1) {
      feedbackButton.updateStyle(feedbackButtonStyle);
    }
    if (alertViewStyle != -1) {
      alertView.updateStyle(alertViewStyle);
    }
  }

  private void applyAttributesForPortrait() {
    instructionLayout.setBackgroundColor(primaryBackgroundColor);
    subStepLayout.setBackgroundColor(listViewBackgroundColor);
    turnLaneLayout.setBackgroundColor(listViewBackgroundColor);
  }

  /**
   * For landscape orientation, manually set the drawable tint based on the customized colors.
   */
  private void applyAttributesForLandscape() {
    instructionLayoutText.setBackgroundColor(primaryBackgroundColor);

    View instructionLayoutManeuver = findViewById(R.id.instructionManeuverLayout);
    instructionLayoutManeuver.setBackgroundColor(primaryBackgroundColor);

    Drawable subStepBackground = DrawableCompat.wrap(subStepLayout.getBackground()).mutate();
    DrawableCompat.setTint(subStepBackground, listViewBackgroundColor);

    Drawable turnLaneBackground = DrawableCompat.wrap(turnLaneLayout.getBackground()).mutate();
    DrawableCompat.setTint(turnLaneBackground, listViewBackgroundColor);
  }

  /**
   * Sets up the {@link RecyclerView} that is used to display the turn lanes.
   */
  private void initializeTurnLaneRecyclerView() {
    turnLaneAdapter = new TurnLaneAdapter(maneuverViewStyle);
    rvTurnLanes.setAdapter(turnLaneAdapter);
    rvTurnLanes.setHasFixedSize(true);
    rvTurnLanes.setLayoutManager(new LinearLayoutManager(getContext(),
        LinearLayoutManager.HORIZONTAL, false));
  }

  /**
   * Sets up the {@link RecyclerView} that is used to display the list of instructions.
   */
  private void initializeInstructionListRecyclerView() {
    instructionListAdapter = new InstructionListAdapter(distanceFormatter);
    instructionListAdapter.setColors(primaryTextColor, secondaryTextColor,
        maneuverViewPrimaryColor, maneuverViewSecondaryColor);
    rvInstructions.setAdapter(instructionListAdapter);
    rvInstructions.setHasFixedSize(true);
    rvInstructions.setLayoutManager(new LinearLayoutManager(getContext()));
  }

  /**
   * Initializes all animations needed to show / hide views.
   */
  private void initializeAnimations() {
    Context context = getContext();
    rerouteSlideDownTop = AnimationUtils.loadAnimation(context, R.anim.slide_down_top);
    rerouteSlideUpTop = AnimationUtils.loadAnimation(context, R.anim.slide_up_top);
  }

  private void updateBannerInstructions(BannerText primaryBanner, BannerText secondaryBanner,
      BannerText subBanner, String currentDrivingSide) {
    if (primaryBanner != null) {
      updateManeuverView(primaryBanner.type(), primaryBanner.modifier(), primaryBanner.degrees(), currentDrivingSide);
      updateDataFromBannerText(primaryBanner, secondaryBanner);
      updateSubStep(subBanner, primaryBanner.modifier(), currentDrivingSide);
    }
  }

  private void onInstructionListVisibilityChanged(boolean visible) {
    if (instructionListListener != null) {
      instructionListListener.onInstructionListVisibilityChanged(visible);
    }
  }

  private void addBottomSheetListener() {
    FragmentManager fragmentManager = obtainSupportFragmentManager();
    if (fragmentManager != null) {
      String tag = FeedbackBottomSheet.TAG;
      FeedbackBottomSheet feedbackBottomSheet = (FeedbackBottomSheet) fragmentManager.findFragmentByTag(tag);
      if (feedbackBottomSheet != null) {
        feedbackBottomSheet.setFeedbackBottomSheetListener(this);
      }
    }
  }

  private void subscribeAlertView() {
    alertView.subscribe(navigationViewModel);
  }

  private void initializeButtonListeners() {
    feedbackButton.addOnClickListener(view -> showFeedbackBottomSheet());
    soundButton.addOnClickListener(view -> navigationViewModel.setMuted(soundButton.toggleMute()));
  }

  private void showButtons() {
    feedbackButton.show();
    soundButton.show();
  }

  private void initializeStepListClickListener() {
    if (ViewUtils.isLandscape(getContext())) {
      initializeLandscapeListListener();
    } else {
      initializePortraitListListener();
    }
  }

  private void initializeButtons() {
    feedbackButton.hide();
    soundButton.hide();
  }

  /**
   * For portrait orientation, attach the listener to the whole layout
   * and use custom animations to hide and show the instructions /sound layout
   */
  private void initializePortraitListListener() {
    instructionLayout.setOnClickListener(instructionView -> {
      boolean instructionsVisible = instructionListLayout.getVisibility() == VISIBLE;
      if (!instructionsVisible) {
        showInstructionList();
      } else {
        hideInstructionList();
      }
    });
  }

  /**
   * For landscape orientation, the click listener is attached to
   * the instruction text layout and the constraints are adjusted before animating
   */
  private void initializeLandscapeListListener() {
    instructionLayoutText.setOnClickListener(instructionLayoutText -> {
      boolean instructionsVisible = instructionListLayout.getVisibility() == VISIBLE;
      if (!instructionsVisible) {
        showInstructionList();
      } else {
        hideInstructionList();
      }
    });
  }

  /**
   * Looks to see if we have a new distance text.
   *
   * @param model provides distance text
   */
  private boolean newDistanceText(InstructionModel model) {
    return !stepDistanceText.getText().toString().isEmpty()
        && !TextUtils.isEmpty(model.retrieveStepDistanceRemaining())
        && !stepDistanceText.getText().toString()
        .contentEquals(model.retrieveStepDistanceRemaining().toString());
  }

  /**
   * Sets current distance text.
   *
   * @param model provides distance text
   */
  private void distanceText(InstructionModel model) {
    stepDistanceText.setText(model.retrieveStepDistanceRemaining());
  }

  private InstructionLoader createInstructionLoader(TextView textView, BannerText bannerText) {
    if (hasComponents(bannerText)) {
      return new InstructionLoader(textView, bannerText);
    } else {
      return null;
    }
  }

  private boolean hasComponents(BannerText bannerText) {
    return bannerText != null && bannerText.components() != null && !bannerText.components().isEmpty();
  }

  /**
   * Looks to see if we have a new step.
   *
   * @param routeProgress provides updated step information
   * @return true if new step, false if not
   */
  private boolean newStep(RouteProgress routeProgress) {
    boolean newStep = currentStep == null
        || !currentStep.equals(routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStep());
    currentStep = routeProgress.getCurrentLegProgress().getCurrentStepProgress().getStep();
    return newStep;
  }

  private void updateSubStep(BannerText subText, String primaryManeuverModifier, String drivingSide) {
    if (shouldShowSubStep(subText)) {
      String maneuverType = subText.type();
      String maneuverModifier = subText.modifier();
      subManeuverView.setManeuverTypeAndModifier(maneuverType, maneuverModifier);
      Double roundaboutAngle = subText.degrees();
      if (roundaboutAngle != null) {
        subManeuverView.setRoundaboutAngle(roundaboutAngle.floatValue());
      }
      subManeuverView.setDrivingSide(drivingSide);
      InstructionLoader instructionLoader = createInstructionLoader(subStepText, subText);
      if (instructionLoader != null) {
        instructionLoader.loadInstruction();
      }
      showSubLayout();
      hideTurnLanes();
      return;
    } else {
      hideSubLayout();
    }

    if (shouldShowTurnLanes(subText, primaryManeuverModifier)) {
      turnLaneAdapter.addTurnLanes(subText.components(), primaryManeuverModifier);
      showTurnLanes();
    } else {
      hideTurnLanes();
    }
  }

  private boolean shouldShowSubStep(@Nullable BannerText subText) {
    return subText != null
        && subText.type() != null
        && !subText.type().contains(COMPONENT_TYPE_LANE);
  }

  private void showSubLayout() {
    if (!(subStepLayout.getVisibility() == VISIBLE)) {
      beginDelayedTransition();
      subStepLayout.setVisibility(VISIBLE);
    }
  }

  private void hideSubLayout() {
    if (subStepLayout.getVisibility() == VISIBLE) {
      beginDelayedTransition();
      subStepLayout.setVisibility(GONE);
    }
  }

  private void animateShowGuidanceViewImage() {
    if (guidanceViewImage.getVisibility() == GONE) {
      beginGuidanceImageDelayedTransition();
      guidanceViewImage.setVisibility(VISIBLE);
    }
  }

  private void animateHideGuidanceViewImage() {
    if (guidanceViewImage.getVisibility() == VISIBLE) {
      beginGuidanceImageDelayedTransition();
      guidanceViewImage.setVisibility(GONE);
    }
  }

  private boolean shouldShowTurnLanes(BannerText subText, String maneuverModifier) {
    if (!hasComponents(subText) || TextUtils.isEmpty(maneuverModifier)) {
      return false;
    }
    for (BannerComponents components : subText.components()) {
      if (components.type().equals(COMPONENT_TYPE_LANE)) {
        return true;
      }
    }
    return false;
  }

  private void showTurnLanes() {
    if (turnLaneLayout.getVisibility() == GONE) {
      beginDelayedTransition();
      turnLaneLayout.setVisibility(VISIBLE);
    }
  }

  private void hideTurnLanes() {
    if (turnLaneLayout.getVisibility() == VISIBLE) {
      beginDelayedTransition();
      turnLaneLayout.setVisibility(GONE);
    }
  }

  @Nullable
  private FragmentManager obtainSupportFragmentManager() {
    try {
      return ((FragmentActivity) getContext()).getSupportFragmentManager();
    } catch (ClassCastException exception) {
      Timber.e(exception);
      return null;
    }
  }

  /**
   * Adjust the banner text layout {@link ConstraintLayout} vertical bias.
   *
   * @param percentBias to be set to the text layout
   */
  private void adjustBannerTextVerticalBias(float percentBias) {
    if (!ViewUtils.isLandscape(getContext())) {
      ConstraintLayout.LayoutParams params = (ConstraintLayout.LayoutParams) instructionLayoutText.getLayoutParams();
      params.verticalBias = percentBias;
      instructionLayoutText.setLayoutParams(params);
    }
  }

  private void beginDelayedTransition() {
    TransitionManager.beginDelayedTransition(this);
  }

  private void beginGuidanceImageDelayedTransition() {
    AutoTransition transition = new AutoTransition();
    transition.setDuration(GUIDANCE_VIEW_TRANSITION_SPEED);
    transition.setInterpolator(new DecelerateInterpolator());
    TransitionManager.beginDelayedTransition(this, transition);
  }

  private void cancelDelayedTransition() {
    clearAnimation();
  }

  private void updateDataFromInstruction(InstructionModel model) {
    updateDistanceText(model);
    updateInstructionList(model);
    if (newStep(model.retrieveProgress())) {
      LegStep upComingStep = model.retrieveProgress().getCurrentLegProgress().getUpcomingStep();
      ImageCreator.getInstance().prefetchImageCache(upComingStep);
    }
  }

  /**
   * Looks to see if we have a new instruction text.
   * Sets new instruction text if found.
   */
  private void updateDataFromBannerText(@NonNull BannerText primaryBannerText, BannerText secondaryBannerText) {
    if (secondaryBannerText == null) {
      loadPrimary(primaryBannerText);
      return;
    }
    loadPrimaryAndSecondary(primaryBannerText, secondaryBannerText);
  }

  private void loadPrimary(BannerText primaryBannerText) {
    if (!ViewUtils.isLandscape(getContext())) {
      stepPrimaryText.setMaxLines(2);
    }
    stepSecondaryText.setVisibility(GONE);
    adjustBannerTextVerticalBias(0.5f);

    if (ViewUtils.isLandscape(getContext()) && instructionLayoutText.getWidth() > 0) {
      stepPrimaryText.setMaxWidth(instructionLayoutText.getWidth());
    }
    loadTextWith(primaryBannerText, stepPrimaryText);
  }

  private void loadPrimaryAndSecondary(BannerText primaryBannerText, BannerText secondaryBannerText) {
    stepPrimaryText.setMaxLines(1);
    stepSecondaryText.setVisibility(VISIBLE);
    adjustBannerTextVerticalBias(0.65f);

    if (ViewUtils.isLandscape(getContext())) {
      int primaryTextMaxWidth = (int) (instructionLayoutText.getWidth()
          * MAXIMUM_PRIMARY_INSTRUCTION_TEXT_WIDTH_RATIO_IN_LANDSCAPE);
      if (primaryTextMaxWidth > 0) {
        stepPrimaryText.setMaxWidth(primaryTextMaxWidth);
      }
    }
    loadTextWith(primaryBannerText, stepPrimaryText);

    if (ViewUtils.isLandscape(getContext())) {
      int secondaryTextMaxWidth = instructionLayoutText.getWidth()
          - (int) stepPrimaryText.getPaint().measureText(String.valueOf(stepPrimaryText.getText()));
      if (secondaryTextMaxWidth > 0) {
        stepSecondaryText.setMaxWidth(secondaryTextMaxWidth);
      }
    }
    loadTextWith(secondaryBannerText, stepSecondaryText);
  }

  private void loadTextWith(BannerText bannerText, TextView textView) {
    InstructionLoader instructionLoader = createInstructionLoader(textView, bannerText);
    if (instructionLoader != null) {
      instructionLoader.loadInstruction();
    }
  }

  /**
   * Looks to see if we have a new maneuver modifier or type.
   * Updates new maneuver image if one is found.
   */
  private void updateManeuverView(String maneuverViewType, String maneuverViewModifier,
      @Nullable Double roundaboutAngle, String drivingSide) {
    maneuverView.setManeuverTypeAndModifier(maneuverViewType, maneuverViewModifier);
    if (roundaboutAngle != null) {
      maneuverView.setRoundaboutAngle(roundaboutAngle.floatValue());
    }
    maneuverView.setDrivingSide(drivingSide);
  }

  /**
   * Looks to see if we have a new distance text.
   * Sets new distance text if found.
   *
   * @param model provides distance text
   */
  private void updateDistanceText(InstructionModel model) {
    if (newDistanceText(model)) {
      distanceText(model);
    } else if (stepDistanceText.getText().toString().isEmpty()) {
      distanceText(model);
    }
  }

  private void updateLandscapeConstraintsTo(int layoutRes) {
    final int feedbackButtonVisibility = feedbackButton.getVisibility();
    final int soundButtonVisibility = feedbackButton.getVisibility();
    final int subInstructionLayoutVisibility = subStepLayout.getVisibility();
    final int turnLaneLayoutVisibility = turnLaneLayout.getVisibility();

    ConstraintSet collapsed = new ConstraintSet();
    collapsed.clone(getContext(), layoutRes);
    collapsed.applyTo(instructionLayout);

    feedbackButton.setVisibility(feedbackButtonVisibility);
    soundButton.setVisibility(soundButtonVisibility);
    subStepLayout.setVisibility(subInstructionLayoutVisibility);
    turnLaneLayout.setVisibility(turnLaneLayoutVisibility);
  }

  /**
   * Used to update the instructions list with the current steps.
   *
   * @param model to provide the current steps and unit type
   */
  private void updateInstructionList(InstructionModel model) {
    RouteProgress routeProgress = model.retrieveProgress();
    boolean isListShowing = instructionListLayout.getVisibility() == VISIBLE;
    rvInstructions.stopScroll();
    instructionListAdapter.updateBannerListWith(routeProgress, isListShowing);
  }

  /**
   * Get driving side.
   * The drivingSide from {@link RouteProgress} has higher priority than user's fallback setting.
   * Driving side right will be the default value if none of above is available.
   *
   * @return the driving side
   */
  private String getDrivingSide(String drivingSideFallback) {
    if (currentStep != null) {
      return currentStep.drivingSide();
    } else if (drivingSideFallback != null) {
      return drivingSideFallback;
    } else {
      return ManeuverModifier.RIGHT;
    }
  }

  private Animation.AnimationListener getInstructionListAnimationListener() {
    return new Animation.AnimationListener() {
      @Override
      public void onAnimationStart(Animation animation) {}

      @Override
      public void onAnimationEnd(Animation animation) {
        rvInstructions.stopScroll();
        rvInstructions.smoothScrollToPosition(0);
      }

      @Override
      public void onAnimationRepeat(Animation animation) {}
    };
  }
}
