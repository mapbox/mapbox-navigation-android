package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.support.constraint.ConstraintSet;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.DefaultItemAnimator;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.transition.TransitionManager;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewParent;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.AnimationUtils;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.mapbox.services.android.navigation.ui.v5.NavigationView;
import com.mapbox.services.android.navigation.ui.v5.NavigationViewModel;
import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.ThemeSwitcher;
import com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneAdapter;
import com.mapbox.services.android.navigation.ui.v5.summary.list.InstructionListAdapter;
import com.mapbox.services.android.navigation.v5.navigation.NavigationConstants;
import com.mapbox.services.android.navigation.v5.offroute.OffRouteListener;
import com.mapbox.services.android.navigation.v5.routeprogress.ProgressChangeListener;
import com.mapbox.services.android.navigation.v5.routeprogress.RouteProgress;
import com.mapbox.services.android.navigation.v5.utils.abbreviation.StringAbbreviator;

import java.text.DecimalFormat;

/**
 * A view that can be used to display upcoming maneuver information and control
 * voice instruction mute / unmute.
 * <p>
 * An {@link ImageView} is used to display the maneuver image on the left.
 * Two {@link TextView}s are used to display distance to the next maneuver, as well
 * as the name of the destination / maneuver name / instruction based on what data is available
 * <p>
 * To automatically have this view update with information from
 * {@link com.mapbox.services.android.navigation.v5.navigation.MapboxNavigation},
 * add the view as a {@link ProgressChangeListener} and / or {@link OffRouteListener}
 *
 * @since 0.6.0
 */
public class InstructionView extends RelativeLayout {

  public boolean isMuted;
  private ImageView maneuverImage;
  private TextView stepDistanceText;
  private TextView stepPrimaryText;
  private TextView stepSecondaryText;
  private TextView soundChipText;
  private FloatingActionButton soundFab;
  private View rerouteLayout;
  private View turnLaneLayout;
  private RecyclerView rvTurnLanes;
  private RecyclerView rvInstructions;
  private TurnLaneAdapter turnLaneAdapter;
  private View soundLayout;
  private View feedbackLayout;
  private ConstraintLayout instructionLayout;
  private View instructionListLayout;
  private InstructionListAdapter instructionListAdapter;
  private Animation rerouteSlideUpTop;
  private Animation rerouteSlideDownTop;
  private AnimationSet fadeInSlowOut;
  private DecimalFormat decimalFormat;
  private int currentManeuverId;
  private boolean isRerouting;

  public InstructionView(Context context) {
    this(context, null);
  }

  public InstructionView(Context context, @Nullable AttributeSet attrs) {
    this(context, attrs, -1);
  }

  public InstructionView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
    super(context, attrs, defStyleAttr);
    init();
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
    initBackground();
    initTurnLaneRecyclerView();
    initDirectionsRecyclerView();
    initDecimalFormat();
    initAnimations();
    initClickListener();
  }

  /**
   * Subscribes to a {@link NavigationViewModel} for
   * updates from {@link android.arch.lifecycle.LiveData}.
   * <p>
   * Updates all views with fresh data / shows & hides re-route state.
   *
   * @param navigationViewModel to which this View is subscribing
   * @since 0.6.2
   */
  public void subscribe(NavigationViewModel navigationViewModel) {
    navigationViewModel.instructionModel.observe((LifecycleOwner) getContext(), new Observer<InstructionModel>() {
      @Override
      public void onChanged(@Nullable InstructionModel instructionModel) {
        if (instructionModel != null) {
          addManeuverImage(instructionModel);
          addDistanceText(instructionModel);
          addTextInstruction(instructionModel);
          addTurnLanes(instructionModel);
          updateSteps(instructionModel.getProgress());
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
            instructionListAdapter.clear();
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
   * @param routeProgress used to provide navigation / progress data
   * @since 0.6.2
   */
  @SuppressWarnings("UnusedDeclaration")
  public void update(RouteProgress routeProgress) {
    if (routeProgress != null && !isRerouting) {
      InstructionModel model = new InstructionModel(routeProgress, decimalFormat);
      addManeuverImage(model);
      addDistanceText(model);
      addTextInstruction(model);
      addTurnLanes(model);
    }
  }

  /**
   * Will slide the reroute view down from the top of the screen
   * and make it visible
   *
   * @since 0.6.0
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
   *
   * @since 0.6.0
   */
  public void hideRerouteState() {
    if (rerouteLayout.getVisibility() == VISIBLE) {
      rerouteLayout.startAnimation(rerouteSlideUpTop);
      rerouteLayout.setVisibility(INVISIBLE);
    }
  }

  /**
   * Will toggle the view between muted and unmuted states.
   *
   * @return boolean true if muted, false if not
   * @since 0.6.0
   */
  public boolean toggleMute() {
    return isMuted ? unmute() : mute();
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
   * Hide the instruction list and show the sound button.
   * <p>
   * This is based on orientation so the different layouts (for portrait vs. landscape)
   * can be animated appropriately.
   */
  public void hideInstructionList() {
    int orientation = getContext().getResources().getConfiguration().orientation;
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      ConstraintSet collapsed = new ConstraintSet();
      collapsed.clone(getContext(), R.layout.instruction_layout);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        TransitionManager.beginDelayedTransition(InstructionView.this);
      }
      collapsed.applyTo(instructionLayout);
      instructionListLayout.setVisibility(INVISIBLE);
      soundLayout.setVisibility(VISIBLE);
      feedbackLayout.setVisibility(VISIBLE);
    } else {
      Animation slideInRight = AnimationUtils.loadAnimation(getContext(), R.anim.slide_in_right);
      Animation slideUp = AnimationUtils.loadAnimation(getContext(), R.anim.slide_up_top);
      slideUp.setInterpolator(new AccelerateInterpolator());
      soundLayout.setVisibility(VISIBLE);
      soundLayout.startAnimation(slideInRight);
      feedbackLayout.setVisibility(VISIBLE);
      feedbackLayout.startAnimation(slideInRight);
      instructionListLayout.startAnimation(slideUp);
      instructionListLayout.setVisibility(INVISIBLE);
    }
    showNavigationViewRecenterBtn();
  }

  /**
   * Show the instruction list and hide the sound button.
   * <p>
   * This is based on orientation so the different layouts (for portrait vs. landscape)
   * can be animated appropriately.
   */
  public void showInstructionList() {
    int orientation = getContext().getResources().getConfiguration().orientation;
    if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
      ConstraintSet expanded = new ConstraintSet();
      expanded.clone(getContext(), R.layout.instruction_layout_alt);
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
        TransitionManager.beginDelayedTransition(InstructionView.this);
      }
      expanded.applyTo(instructionLayout);
      instructionListLayout.setVisibility(VISIBLE);
      soundLayout.setVisibility(INVISIBLE);
      feedbackLayout.setVisibility(INVISIBLE);
    } else {
      Animation slideDown = AnimationUtils.loadAnimation(getContext(), R.anim.slide_down_top);
      slideDown.setInterpolator(new DecelerateInterpolator());
      Animation slideOutRight = AnimationUtils.loadAnimation(getContext(), R.anim.slide_out_right);
      instructionListLayout.setVisibility(VISIBLE);
      instructionListLayout.startAnimation(slideDown);
      soundLayout.startAnimation(slideOutRight);
      soundLayout.setVisibility(INVISIBLE);
      feedbackLayout.startAnimation(slideOutRight);
      feedbackLayout.setVisibility(INVISIBLE);
    }
    hideNavigationViewRecenterBtn();
  }

  /**
   * If the parent is an instance of {@link NavigationView},
   * show the recenter button.
   */
  private void showNavigationViewRecenterBtn() {
    ViewParent parent = getParent().getParent();
    if (parent instanceof NavigationView) {
      ((NavigationView) parent).showRecenterBtn();
    }
  }

  /**
   * If the parent is an instance of {@link NavigationView},
   * hide the recenter button.
   */
  private void hideNavigationViewRecenterBtn() {
    ViewParent parent = getParent().getParent();
    if (parent instanceof NavigationView) {
      ((NavigationView) parent).hideRecenterBtn();
    }
  }

  /**
   * Inflates this layout needed for this view.
   */
  private void init() {
    inflate(getContext(), R.layout.instruction_view_layout, this);
  }

  /**
   * Finds and binds all necessary views
   */
  private void bind() {
    maneuverImage = findViewById(R.id.maneuverImageView);
    stepDistanceText = findViewById(R.id.stepDistanceText);
    stepPrimaryText = findViewById(R.id.stepPrimaryText);
    stepSecondaryText = findViewById(R.id.stepSecondaryText);
    soundChipText = findViewById(R.id.soundText);
    soundFab = findViewById(R.id.soundFab);
    rerouteLayout = findViewById(R.id.rerouteLayout);
    turnLaneLayout = findViewById(R.id.turnLaneLayout);
    rvTurnLanes = findViewById(R.id.rvTurnLanes);
    soundLayout = findViewById(R.id.soundLayout);
    feedbackLayout = findViewById(R.id.feedbackLayout);
    instructionLayout = findViewById(R.id.instructionLayout);
    instructionListLayout = findViewById(R.id.instructionListLayout);
    rvInstructions = findViewById(R.id.rvInstructions);
    initInstructionAutoSize();
  }

  /**
   * For API 21 and lower, manually set the drawable tint based on the colors
   * set in the given navigation theme (light or dark).
   */
  private void initBackground() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
      int navigationViewPrimaryColor = ThemeSwitcher.retrieveNavigationViewPrimaryColor(getContext());
      int navigationViewSecondaryColor = ThemeSwitcher.retrieveNavigationViewSecondaryColor(getContext());
      int navigationViewBannerBackgroundColor = ThemeSwitcher.retrieveNavigationViewBannerBackgroundColor(getContext());
      // Instruction Layout banner - banner background
      if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
        View instructionLayoutText = findViewById(R.id.instructionLayoutText);
        View instructionLayoutManeuver = findViewById(R.id.instructionLayoutManeuver);
        Drawable textBackground = DrawableCompat.wrap(instructionLayoutText.getBackground()).mutate();
        Drawable maneuverBackground = DrawableCompat.wrap(instructionLayoutManeuver.getBackground()).mutate();
        DrawableCompat.setTint(textBackground, navigationViewBannerBackgroundColor);
        DrawableCompat.setTint(maneuverBackground, navigationViewBannerBackgroundColor);
      } else {
        View instructionLayout = findViewById(R.id.instructionLayout);
        Drawable instructionBackground = DrawableCompat.wrap(instructionLayout.getBackground()).mutate();
        DrawableCompat.setTint(instructionBackground, navigationViewBannerBackgroundColor);
      }
      // Sound chip text - primary
      Drawable soundChipBackground = DrawableCompat.wrap(soundChipText.getBackground()).mutate();
      DrawableCompat.setTint(soundChipBackground, navigationViewPrimaryColor);
      // Reroute Layout - secondary
      Drawable rerouteBackground = DrawableCompat.wrap(rerouteLayout.getBackground()).mutate();
      DrawableCompat.setTint(rerouteBackground, navigationViewSecondaryColor);
    }
  }

  /**
   * Sets up mute UI event.
   * <p>
   * Shows chip with "Muted" text.
   * Changes sound {@link FloatingActionButton}
   * {@link android.graphics.drawable.Drawable} to denote sound is off.
   * <p>
   * Sets private state variable to true (muted)
   *
   * @return true, view is in muted state
   */
  private boolean mute() {
    isMuted = true;
    setSoundChipText(getContext().getString(R.string.muted));
    showSoundChip();
    soundFabOff();
    return isMuted;
  }

  /**
   * Sets up unmuted UI event.
   * <p>
   * Shows chip with "Unmuted" text.
   * Changes sound {@link FloatingActionButton}
   * {@link android.graphics.drawable.Drawable} to denote sound is on.
   * <p>
   * Sets private state variable to false (unmuted)
   *
   * @return false, view is in unmuted state
   */
  private boolean unmute() {
    isMuted = false;
    setSoundChipText(getContext().getString(R.string.unmuted));
    showSoundChip();
    soundFabOn();
    return isMuted;
  }

  /**
   * Changes sound {@link FloatingActionButton}
   * {@link android.graphics.drawable.Drawable} to denote sound is off.
   */
  private void soundFabOff() {
    soundFab.setImageResource(R.drawable.ic_sound_off);
  }

  /**
   * Changes sound {@link FloatingActionButton}
   * {@link android.graphics.drawable.Drawable} to denote sound is on.
   */
  private void soundFabOn() {
    soundFab.setImageResource(R.drawable.ic_sound_on);
  }

  /**
   * Sets {@link TextView} inside of chip view.
   *
   * @param text to be displayed in chip view ("Muted"/"Umuted")
   */
  private void setSoundChipText(String text) {
    soundChipText.setText(text);
  }

  /**
   * Shows and then hides the sound chip using {@link AnimationSet}
   */
  private void showSoundChip() {
    soundChipText.startAnimation(fadeInSlowOut);
  }

  /**
   * Called after we bind the views, this will allow the step instruction {@link TextView}
   * to automatically re-size based on the length of the text.
   */
  private void initInstructionAutoSize() {
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepPrimaryText,
      24, 30, 1, TypedValue.COMPLEX_UNIT_SP);
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepSecondaryText,
      20, 26, 1, TypedValue.COMPLEX_UNIT_SP);
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepDistanceText,
      16, 20, 1, TypedValue.COMPLEX_UNIT_SP);
  }

  /**
   * Sets up the {@link RecyclerView} that is used to display the turn lanes.
   */
  private void initTurnLaneRecyclerView() {
    turnLaneAdapter = new TurnLaneAdapter();
    rvTurnLanes.setAdapter(turnLaneAdapter);
    rvTurnLanes.setHasFixedSize(true);
    rvTurnLanes.setLayoutManager(new LinearLayoutManager(getContext(),
      LinearLayoutManager.HORIZONTAL, false));
  }

  /**
   * Sets up the {@link RecyclerView} that is used to display the list of instructions.
   */
  private void initDirectionsRecyclerView() {
    instructionListAdapter = new InstructionListAdapter();
    rvInstructions.setAdapter(instructionListAdapter);
    rvInstructions.setHasFixedSize(true);
    rvInstructions.setNestedScrollingEnabled(true);
    rvInstructions.setItemAnimator(new DefaultItemAnimator());
    rvInstructions.setLayoutManager(new LinearLayoutManager(getContext()));
  }

  /**
   * Initializes decimal format to be used to populate views with
   * distance remaining.
   */
  private void initDecimalFormat() {
    decimalFormat = new DecimalFormat(NavigationConstants.DECIMAL_FORMAT);
  }

  /**
   * Initializes all animations needed to show / hide views.
   */
  private void initAnimations() {
    Context context = getContext();
    rerouteSlideDownTop = AnimationUtils.loadAnimation(context, R.anim.slide_down_top);
    rerouteSlideUpTop = AnimationUtils.loadAnimation(context, R.anim.slide_up_top);

    Animation fadeIn = new AlphaAnimation(0, 1);
    fadeIn.setInterpolator(new DecelerateInterpolator());
    fadeIn.setDuration(300);

    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setInterpolator(new AccelerateInterpolator());
    fadeOut.setStartOffset(1000);
    fadeOut.setDuration(1000);

    fadeInSlowOut = new AnimationSet(false);
    fadeInSlowOut.addAnimation(fadeIn);
    fadeInSlowOut.addAnimation(fadeOut);
  }

  private void initClickListener() {
    if (getContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE) {
      initLandscapeClickListener();
    } else {
      initPortraitClickListener();
    }
  }

  /**
   * For portrait orientation, attach the listener to the whole layout
   * and use custom animations to hide and show the instructions /sound layout
   */
  private void initPortraitClickListener() {
    instructionLayout.setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View instructionView) {
        boolean instructionsVisible = instructionListLayout.getVisibility() == VISIBLE;
        if (!instructionsVisible) {
          showInstructionList();
        } else {
          hideInstructionList();
        }
      }
    });
  }

  /**
   * For landscape orientation, the click listener is attached to
   * the instruction text layout and the constraints are adjusted before animating
   */
  private void initLandscapeClickListener() {
    findViewById(R.id.instructionLayoutText).setOnClickListener(new OnClickListener() {
      @Override
      public void onClick(View instructionLayoutText) {
        boolean instructionsVisible = instructionListLayout.getVisibility() == VISIBLE;
        if (!instructionsVisible) {
          showInstructionList();
        } else {
          hideInstructionList();
        }
      }
    });
  }

  /**
   * Looks to see if we have a new image id.
   * Sets new image resource if one is found.
   *
   * @param model provides maneuver image id
   */
  private void addManeuverImage(InstructionModel model) {
    if (currentManeuverId != model.getManeuverImage()) {
      currentManeuverId = model.getManeuverImage();
      maneuverImage.setImageResource(model.getManeuverImage());
    }
  }

  /**
   * Looks to see if we have a new distance text.
   * Sets new distance text if found.
   *
   * @param model provides distance text
   */
  private void addDistanceText(InstructionModel model) {
    if (newDistanceText(model)) {
      distanceText(model);
    } else if (stepDistanceText.getText().toString().isEmpty()) {
      distanceText(model);
    }
  }

  /**
   * Looks to see if we have a new distance text.
   *
   * @param model provides distance text
   */
  private boolean newDistanceText(InstructionModel model) {
    return !stepDistanceText.getText().toString().isEmpty()
      && !TextUtils.isEmpty(model.getStepDistanceRemaining())
      && !stepDistanceText.getText().toString()
      .contentEquals(model.getStepDistanceRemaining().toString());
  }

  /**
   * Sets current distance text.
   *
   * @param model provides distance text
   */
  private void distanceText(InstructionModel model) {
    stepDistanceText.setText(model.getStepDistanceRemaining());
  }

  /**
   * Looks to see if we have a new instruction text.
   * Sets new instruction text if found.
   *
   * @param model provides instruction text
   */
  private void addTextInstruction(InstructionModel model) {
    if (newPrimaryText(model) || newSecondaryText(model)) {
      textInstructions(model);
    } else if (stepPrimaryText.getText().toString().isEmpty()
      || stepSecondaryText.getText().toString().isEmpty()) {
      textInstructions(model);
    }
  }

  /**
   * Looks to see if we have a new primary instruction text.
   *
   * @param model provides primary instruction text
   */
  private boolean newPrimaryText(InstructionModel model) {
    // New primaryText instruction
    String currentPrimaryText = stepPrimaryText.getText().toString();
    return !currentPrimaryText.isEmpty()
      && !TextUtils.isEmpty(model.getPrimaryText())
      && !currentPrimaryText.contentEquals(model.getPrimaryText());
  }

  /**
   * Looks to see if we have a new secondary instruction text.
   *
   * @param model provides secondary instruction text
   */
  private boolean newSecondaryText(InstructionModel model) {
    // New primaryText instruction
    String currentSecondaryText = stepSecondaryText.getText().toString();
    return !currentSecondaryText.isEmpty()
      && !TextUtils.isEmpty(model.getSecondaryText())
      && !currentSecondaryText.contentEquals(model.getSecondaryText());
  }

  /**
   * Sets current instruction text.
   *
   * @param model provides instruction text
   */
  private void textInstructions(InstructionModel model) {
    if (!TextUtils.isEmpty(model.getPrimaryText())) {
      stepPrimaryText.setText(StringAbbreviator.abbreviate(model.getPrimaryText()));
    }
    if (!TextUtils.isEmpty(model.getSecondaryText())) {
      if (stepSecondaryText.getVisibility() == GONE) {
        stepSecondaryText.setVisibility(VISIBLE);
        stepPrimaryText.setMaxLines(1);
      }
      stepSecondaryText.setText(StringAbbreviator.abbreviate(model.getSecondaryText()));
    } else {
      stepPrimaryText.setMaxLines(2);
      stepSecondaryText.setVisibility(GONE);
    }
  }

  /**
   * Looks for turn lane data and populates / shows the turn lane view if found.
   * If not, hides the turn lane view.
   *
   * @param model created with new {@link RouteProgress} holding turn lane data
   */
  private void addTurnLanes(InstructionModel model) {
    if (model.getTurnLanes() != null
      && !TextUtils.isEmpty(model.getManeuverModifier())) {
      turnLaneAdapter.addTurnLanes(model.getTurnLanes(), model.getManeuverModifier());
      showTurnLanes();
    } else {
      hideTurnLanes();
    }
  }

  /**
   * Shows turn lane view
   */
  private void showTurnLanes() {
    if (turnLaneLayout.getVisibility() == GONE) {
      turnLaneLayout.setVisibility(VISIBLE);
    }
  }

  /**
   * Hides turn lane view
   */
  private void hideTurnLanes() {
    if (turnLaneLayout.getVisibility() == VISIBLE) {
      turnLaneLayout.setVisibility(GONE);
    }
  }

  /**
   * Used to update the instructions list with the current steps.
   *
   * @param routeProgress to provide the current steps
   */
  private void updateSteps(RouteProgress routeProgress) {
    instructionListAdapter.updateSteps(routeProgress);
  }
}
