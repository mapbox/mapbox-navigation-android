package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.widget.TextViewCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.SpannableStringBuilder;
import android.text.TextUtils;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.View;
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
import com.mapbox.services.android.navigation.ui.v5.stylekit.ManeuverView;
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

  private View instructionLayout;
  private ManeuverView maneuverView;
  private TextView stepDistanceText;
  private TextView stepInstructionText;
  private TextView soundChipText;
  private FloatingActionButton soundFab;
  private View rerouteLayout;
  private View turnLaneLayout;
  private RecyclerView rvTurnLanes;
  private TurnLaneAdapter turnLaneAdapter;

  private Animation slideDownTop;
  private Animation rerouteSlideUpTop;
  private Animation rerouteSlideDownTop;
  private AnimationSet fadeInSlowOut;

  private DecimalFormat decimalFormat;
  private String currentInstruction;
  private SpannableStringBuilder currentDistanceText;
  private boolean turnLanesHidden;
  private boolean isRerouting;
  public boolean isMuted;

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
    initDecimalFormat();
    initAnimations();
  }

  @Override
  public Parcelable onSaveInstanceState() {
    super.onSaveInstanceState();
    return createSavedState();
  }

  @Override
  public void onRestoreInstanceState(Parcelable state) {
    if (state instanceof Bundle) {
      setRestoredState((Bundle) state);
    } else {
      super.onRestoreInstanceState(state);
    }
  }

  public void subscribe(NavigationViewModel navigationViewModel) {
    navigationViewModel.instructionModel.observe((NavigationView) getContext(), new Observer<InstructionModel>() {
      @Override
      public void onChanged(@Nullable InstructionModel instructionModel) {
        if (instructionModel != null) {
          addManeuverImage(instructionModel);
          addDistanceText(instructionModel);
          addTextInstruction(instructionModel);
          addTurnLanes(instructionModel);
        }
      }
    });
    navigationViewModel.isOffRoute.observe((NavigationView) getContext(), new Observer<Boolean>() {
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
   * @param routeProgress used to provide navigation / progress data
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
   * If invisible, this method will slide the view down
   * from the top of the screen and set the visibility to visible
   *
   * @since 0.6.0
   */
  public void show() {
    if (this.getVisibility() == INVISIBLE) {
      this.setVisibility(VISIBLE);
      this.startAnimation(slideDownTop);
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
   * Inflates this layout needed for this view.
   */
  private void init() {
    inflate(getContext(), R.layout.instruction_view_layout, this);
  }

  /**
   * Finds and binds all necessary views
   */
  private void bind() {
    maneuverView = findViewById(R.id.maneuverView);
    stepDistanceText = findViewById(R.id.stepDistanceText);
    stepInstructionText = findViewById(R.id.stepInstructionText);
    soundChipText = findViewById(R.id.soundText);
    soundFab = findViewById(R.id.soundFab);
    instructionLayout = findViewById(R.id.instructionLayout);
    rerouteLayout = findViewById(R.id.rerouteLayout);
    turnLaneLayout = findViewById(R.id.turnLaneLayout);
    rvTurnLanes = findViewById(R.id.rvTurnLanes);
    initInstructionAutoSize();
  }

  private void initBackground() {
    if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.LOLLIPOP) {
      int navigationViewPrimaryColor = ThemeSwitcher.retrieveNavigationViewPrimaryColor(getContext());
      int navigationViewSecondaryColor = ThemeSwitcher.retrieveNavigationViewSecondaryColor(getContext());
      // Instruction Layout - primary
      Drawable instructionBackground = DrawableCompat.wrap(instructionLayout.getBackground()).mutate();
      DrawableCompat.setTint(instructionBackground, navigationViewPrimaryColor);
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
    TextViewCompat.setAutoSizeTextTypeUniformWithConfiguration(stepInstructionText,
      16, 28, 2, TypedValue.COMPLEX_UNIT_SP);
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
    slideDownTop = AnimationUtils.loadAnimation(context, R.anim.slide_down_top);
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

  /**
   * Looks to see if we have a new image id.
   * Sets new image resource if one is found.
   *
   * @param model provides maneuver image id
   */
  private void addManeuverImage(InstructionModel model) {
    maneuverView.setManeuverModifier(model.getManeuverModifier());
    maneuverView.setManeuverType(model.getStep().getManeuver().getType());
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
    } else if (currentDistanceText == null) {
      distanceText(model);
    }
  }

  /**
   * Looks to see if we have a new distance text.
   *
   * @param model provides distance text
   */
  private boolean newDistanceText(InstructionModel model) {
    return currentDistanceText != null
      && !TextUtils.isEmpty(model.getStepDistanceRemaining())
      && !currentDistanceText.toString().contentEquals(model.getStepDistanceRemaining().toString());
  }

  /**
   * Sets current distance text.
   *
   * @param model provides distance text
   */
  private void distanceText(InstructionModel model) {
    currentDistanceText = model.getStepDistanceRemaining();
    stepDistanceText.setText(model.getStepDistanceRemaining());
  }

  /**
   * Looks to see if we have a new instruction text.
   * Sets new instruction text if found.
   *
   * @param model provides instruction text
   */
  private void addTextInstruction(InstructionModel model) {
    if (newTextInstruction(model)) {
      textInstruction(model);
    } else if (currentInstruction == null) {
      textInstruction(model);
    }
  }

  /**
   * Looks to see if we have a new instruction text.
   *
   * @param model provides instruction text
   */
  private boolean newTextInstruction(InstructionModel model) {
    return currentInstruction != null
      && !TextUtils.isEmpty(model.getTextInstruction())
      && !currentInstruction.contentEquals(model.getTextInstruction());
  }

  /**
   * Sets current instruction text.
   *
   * @param model provides instruction text
   */
  private void textInstruction(InstructionModel model) {
    currentInstruction = model.getTextInstruction();
    stepInstructionText.setText(StringAbbreviator.abbreviate(model.getTextInstruction()));
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
//      turnLaneAdapter.addTurnLanes(model.getTurnLanes(), model.getManeuverModifier());
      showTurnLanes();
    } else {
      hideTurnLanes();
    }
  }

  /**
   * Shows turn lane view
   */
  private void showTurnLanes() {
    if (turnLanesHidden) {
      turnLanesHidden = false;
      turnLaneLayout.setVisibility(VISIBLE);
    }
  }

  /**
   * Hides turn lane view
   */
  private void hideTurnLanes() {
    if (!turnLanesHidden) {
      turnLanesHidden = true;
      turnLaneLayout.setVisibility(GONE);
    }
  }

  @NonNull
  private Parcelable createSavedState() {
    Bundle state = new Bundle();
    state.putParcelable(getContext().getString(R.string.instruction_super_state), super.onSaveInstanceState());
    state.putInt(getContext().getString(R.string.instruction_visibility), getVisibility());
    return state;
  }

  @SuppressWarnings("WrongConstant")
  private void setRestoredState(Bundle state) {
    this.setVisibility(state.getInt(getContext().getString(R.string.instruction_visibility), getVisibility()));
    Parcelable superState = state.getParcelable(getContext().getString(R.string.instruction_super_state));
    super.onRestoreInstanceState(superState);
  }
}
