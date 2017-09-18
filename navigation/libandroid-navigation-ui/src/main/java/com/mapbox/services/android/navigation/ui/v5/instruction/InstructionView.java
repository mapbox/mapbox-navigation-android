package com.mapbox.services.android.navigation.ui.v5.instruction;

import android.content.Context;
import android.location.Location;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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

import com.mapbox.services.android.navigation.ui.v5.R;
import com.mapbox.services.android.navigation.ui.v5.instruction.turnlane.TurnLaneAdapter;
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
public class InstructionView extends RelativeLayout implements ProgressChangeListener, OffRouteListener {

  private ImageView maneuverImage;
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
  private int currentManeuverId;
  private SpannableStringBuilder currentDistanceText;
  private boolean showingRerouteState;
  private boolean turnLanesHidden;
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
    initTurnLaneRecyclerView();
    initDecimalFormat();
    initAnimations();
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
   * This view will show a view indicating a new route is being retrieved.
   * This same view will be hidden when the new route is received from
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
    showingRerouteState = true;
    rerouteLayout.startAnimation(rerouteSlideDownTop);
  }

  /**
   * Will slide the reroute view up to the top of the screen
   * and hide it
   *
   * @since 0.6.0
   */
  public void hideRerouteState() {
    showingRerouteState = false;
    rerouteLayout.startAnimation(rerouteSlideUpTop);
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
    maneuverImage = findViewById(R.id.maneuverImageView);
    stepDistanceText = findViewById(R.id.stepDistanceText);
    stepInstructionText = findViewById(R.id.stepInstructionText);
    soundChipText = findViewById(R.id.soundText);
    soundFab = findViewById(R.id.soundFab);
    rerouteLayout = findViewById(R.id.rerouteLayout);
    turnLaneLayout = findViewById(R.id.turnLaneLayout);
    rvTurnLanes = findViewById(R.id.rvTurnLanes);
    initInstructionAutoSize();
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
   * Called in {@link ProgressChangeListener}, creates a new model and then
   * uses it to update the views.
   *
   * @param routeProgress used to provide navigation / progress data
   */
  private void update(RouteProgress routeProgress) {
    if (routeProgress != null && !showingRerouteState) {
      InstructionModel model = new InstructionModel(routeProgress, decimalFormat);
      addManeuverImage(model);
      addDistanceText(model);
      addTextInstruction(model);
      addTurnLanes(model);
    }
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
}
