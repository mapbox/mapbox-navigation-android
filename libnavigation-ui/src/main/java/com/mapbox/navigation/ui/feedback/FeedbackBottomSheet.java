package com.mapbox.navigation.ui.feedback;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.LinearInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.AppCompatButton;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetBehavior;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.mapbox.navigation.ui.R;
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent;

import com.mapbox.navigation.ui.internal.utils.ViewUtils;
import org.jetbrains.annotations.NotNull;
import timber.log.Timber;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A BottomSheetDialogFragment shows Feedback UI with different feedback categories.
 * <p>
 * This view takes a {@link FeedbackBottomSheetListener}.
 * The {@link FeedbackBottomSheetListener#onFeedbackSelected(FeedbackItem)} will be called
 * when a feedback category is selected.
 * The {@link FeedbackBottomSheetListener#onFeedbackDismissed()} will be called when this fragment dismiss.
 */
public class FeedbackBottomSheet extends BottomSheetDialogFragment implements Animator.AnimatorListener {

  public static final String TAG = FeedbackBottomSheet.class.getSimpleName();
  private static final String EMPTY_FEEDBACK_DESCRIPTION = "";
  private static final long CLOSE_BOTTOM_SHEET_AFTER = 150L;
  private static final long TIMER_INTERVAL = 1L;
  private static final int GRID_SPAN_PORTRAIT = 3;
  private static final int GRID_SPAN_LANDSCAPE = 6;

  @Nullable
  private FeedbackBottomSheetListener feedbackBottomSheetListener;
  private TextView feedbackBottomSheetTitleText;
  private ImageButton cancelBtn;
  private LinearLayout feedbackMainLayout;
  private RecyclerView feedbackCategories;
  private FeedbackAdapter feedbackCategoryAdapter;
  private RelativeLayout feedbackSubTypesLayout;
  @Nullable
  private FeedbackSubTypeAdapter feedbackSubTypeAdapter;
  private RecyclerView feedbackSubTypes;
  private ProgressBar feedbackProgressBar;
  private ObjectAnimator countdownAnimation;
  private AppCompatButton reportIssueBtn;
  private @FeedbackFlowType int feedbackFlowType = FEEDBACK_MAIN_FLOW;
  private long duration;
  @Nullable
  private CountDownTimer timer = null;
  private FeedbackItem selectedFeedbackItem;
  private Map<String, List<FeedbackSubTypeItem>> feedbackSubTypeMap;
  private Class<? extends FeedbackBottomSheetListener> feedbackBottomSheetListenerClass;
  @Nullable
  private DismissCommand dismissCommand = null;

  @NonNull
  public static FeedbackBottomSheet newInstance(FeedbackBottomSheetListener feedbackBottomSheetListener,
                                                long duration) {
    return newInstance(feedbackBottomSheetListener, FEEDBACK_MAIN_FLOW, duration);
  }

  @NonNull
  public static FeedbackBottomSheet newInstance(FeedbackBottomSheetListener feedbackBottomSheetListener,
                                                @FeedbackFlowType int flowType, long duration) {
    FeedbackBottomSheet feedbackBottomSheet = new FeedbackBottomSheet();
    feedbackBottomSheet.feedbackFlowType = flowType;
    feedbackBottomSheet.setFeedbackBottomSheetListener(feedbackBottomSheetListener);
    feedbackBottomSheet.setDuration(duration);
    feedbackBottomSheet.setRetainInstance(true);
    return feedbackBottomSheet;
  }

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(BottomSheetDialogFragment.STYLE_NO_FRAME, R.style.Theme_Design_BottomSheetDialog);
  }

  @Override
  public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
    return inflater.inflate(R.layout.mapbox_feedback_bottom_sheet, container, false);
  }

  @Override
  public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
    super.onViewCreated(view, savedInstanceState);
    bind(view);
    initTitleTextView();
    initButtons();
    initFeedbackRecyclerView();
    initCountDownAnimation();
  }

  @NonNull
  @Override
  public Dialog onCreateDialog(Bundle savedInstanceState) {
    BottomSheetDialog dialog = (BottomSheetDialog) super.onCreateDialog(savedInstanceState);
    dialog.setOnShowListener(dialog1 -> {
      BottomSheetDialog bottomSheetDialog = (BottomSheetDialog) dialog1;
      FrameLayout bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
      if (bottomSheet != null) {
        BottomSheetBehavior<FrameLayout> behavior = BottomSheetBehavior.from(bottomSheet);
        behavior.setFitToContents(false);
        behavior.setState(BottomSheetBehavior.STATE_HALF_EXPANDED);
        behavior.setSkipCollapsed(true);
        behavior.addBottomSheetCallback(new BottomSheetBehavior.BottomSheetCallback() {
          @Override public void onStateChanged(@NonNull View bottomSheet, int newState) {
            Timber.e("DaiJun newState=%s", newState);
          }

          @Override public void onSlide(@NonNull View bottomSheet, float slideOffset) {
            Timber.e("DaiJun slideOffset=%s", slideOffset);
          }
        });
      }
    });
    return dialog;
  }

  @Override
  public void onDismiss(@NotNull DialogInterface dialog) {
    super.onDismiss(dialog);
    if (feedbackBottomSheetListener != null) {
      feedbackBottomSheetListener.onFeedbackDismissed();
    }
  }

  @Override
  public void onDestroyView() {
    removeListener();
    removeDialogDismissMessage();
    cancelCountdownAnimation();
    super.onDestroyView();
  }

  @Override
  public void onSaveInstanceState(@NonNull Bundle outState) {
    super.onSaveInstanceState(outState);
    if (feedbackBottomSheetListener != null) {
      feedbackBottomSheetListenerClass = feedbackBottomSheetListener.getClass();
    }
  }

  @Override
  public void onResume() {
    super.onResume();
    if (dismissCommand != null) {
      dismissCommand.invoke();
    }
    dismissCommand = null;
  }

  @Override
  public void onAnimationEnd(Animator animation) {
    if (FeedbackBottomSheet.this.isResumed()) {
      FeedbackBottomSheet.this.dismiss();
    } else {
      dismissCommand = delayedDismissCommand;
    }
  }

  //region Unused Listener Methods

  @Override
  public void onAnimationStart(Animator animation) {

  }

  @Override
  public void onAnimationCancel(Animator animation) {

  }

  @Override
  public void onAnimationRepeat(Animator animation) {

  }

  //endregion

  public void setFeedbackBottomSheetListener(FeedbackBottomSheetListener feedbackBottomSheetListener) {
    if (feedbackBottomSheetListenerClass == null || feedbackBottomSheetListenerClass.isInstance(feedbackBottomSheetListener)) {
      this.feedbackBottomSheetListener = feedbackBottomSheetListener;
    }
  }

  /**
   * @param duration in milliseconds, the BottomSheet will show before being dismissed.
   */
  public void setDuration(long duration) {
    this.duration = duration;
  }

  private void bind(@NonNull View bottomSheetView) {
    feedbackBottomSheetTitleText = bottomSheetView.findViewById(R.id.feedbackBottomSheetTitleText);
    cancelBtn = bottomSheetView.findViewById(R.id.cancelBtn);
    cancelBtn.setColorFilter(Color.WHITE);

    feedbackMainLayout = bottomSheetView.findViewById(R.id.feedbackMainLayout);
    feedbackCategories = bottomSheetView.findViewById(R.id.feedbackCategories);

    feedbackSubTypesLayout = bottomSheetView.findViewById(R.id.feedbackSubTypesLayout);
    feedbackSubTypes = bottomSheetView.findViewById(R.id.feedbackSubTypes);
    feedbackProgressBar = bottomSheetView.findViewById(R.id.feedbackProgress);

    reportIssueBtn = bottomSheetView.findViewById(R.id.reportIssueBtn);
  }

  private void initTitleTextView() {
    feedbackBottomSheetTitleText.setText(R.string.mapbox_report_feedback);
  }

  private void initButtons() {
    cancelBtn.setOnClickListener(view -> dismiss());

    reportIssueBtn.setOnClickListener(view -> {
      if (feedbackBottomSheetListener != null) {
        feedbackBottomSheetListener.onFeedbackSelected(selectedFeedbackItem);
      }
      startTimer();
    });
  }

  private void initFeedbackRecyclerView() {
    final Context context = getContext();

    feedbackCategoryAdapter = new FeedbackAdapter(buildFeedbackCategoryList());
    feedbackCategories.setAdapter(feedbackCategoryAdapter);
    feedbackCategories.setOverScrollMode(RecyclerView.OVER_SCROLL_IF_CONTENT_SCROLLS);
    feedbackCategories.addOnItemTouchListener(new FeedbackClickListener(context, feedbackClickListener));
    if (ViewUtils.isLandscape(context)) {
      feedbackCategories.setLayoutManager(new GridLayoutManager(context, GRID_SPAN_LANDSCAPE));
    } else {
      feedbackCategories.setLayoutManager(new GridLayoutManager(context, GRID_SPAN_PORTRAIT));
    }
  }

  private void initCountDownAnimation() {
    countdownAnimation = ObjectAnimator.ofInt(feedbackProgressBar,
      "progress", 0);
    countdownAnimation.setInterpolator(new LinearInterpolator());
    countdownAnimation.setDuration(duration);
    countdownAnimation.addListener(this);
    countdownAnimation.start();
  }

  private void removeListener() {
    feedbackBottomSheetListener = null;
  }

  private void removeDialogDismissMessage() {
    Dialog dialog = getDialog();
    if (dialog != null && getRetainInstance()) {
      dialog.setDismissMessage(null);
    }
  }

  private void cancelCountdownAnimation() {
    if (countdownAnimation != null) {
      countdownAnimation.removeAllListeners();
      countdownAnimation.cancel();
    }
  }

  private void startTimer() {
    if (timer != null) {
      timer.cancel();
    }
    timer = new CountDownTimer(CLOSE_BOTTOM_SHEET_AFTER, TIMER_INTERVAL) {
      @Override
      public void onTick(long millisUntilFinished) {
        // We don't need to observe changes in interval, hence left empty
      }

      @Override
      public void onFinish() {
        dismiss();
      }
    };
    timer.start();
  }

  @NonNull
  @SuppressLint("WrongConstant")
  private List<FeedbackItem> buildFeedbackCategoryList() {
    List<FeedbackItem> list = new ArrayList<>();

    list.add(new FeedbackItem(getResources().getString(R.string.mapbox_feedback_type_looks_incorrect),
      R.drawable.mapbox_ic_feedback_looks_incorrect,
      FeedbackEvent.INCORRECT_VISUAL_GUIDANCE,
      EMPTY_FEEDBACK_DESCRIPTION));
    list.add(new FeedbackItem(getResources().getString(R.string.mapbox_feedback_type_confusing_audio),
      R.drawable.mapbox_ic_feedback_confusing_audio,
      FeedbackEvent.INCORRECT_AUDIO_GUIDANCE,
      EMPTY_FEEDBACK_DESCRIPTION));
    list.add(new FeedbackItem(getResources().getString(R.string.mapbox_feedback_type_positioning_issue),
        R.drawable.mapbox_ic_feedback_positioning_issue,
        FeedbackEvent.POSITIONING_ISSUE,
        EMPTY_FEEDBACK_DESCRIPTION));
    list.add(new FeedbackItem(getResources().getString(R.string.mapbox_feedback_type_route_quality),
        R.drawable.mapbox_ic_feedback_route_quality,
        FeedbackEvent.ROUTING_ERROR,
        EMPTY_FEEDBACK_DESCRIPTION));
    list.add(new FeedbackItem(getResources().getString(R.string.mapbox_feedback_type_illegal_route),
        R.drawable.mapbox_ic_feedback_illegal_route,
        FeedbackEvent.NOT_ALLOWED,
        EMPTY_FEEDBACK_DESCRIPTION));
    list.add(new FeedbackItem(getResources().getString(R.string.mapbox_feedback_type_road_closure),
        R.drawable.mapbox_ic_feedback_road_closure,
        FeedbackEvent.ROAD_CLOSED,
        EMPTY_FEEDBACK_DESCRIPTION));

    return list;
  }

  @NonNull
  private FeedbackClickListener.ClickCallback feedbackClickListener = new FeedbackClickListener.ClickCallback() {
    @Override
    public void onFeedbackItemClick(@Nullable ImageView view, int feedbackPosition) {
      if (view != null) {
        view.setPressed(!view.isPressed());
      }
      FeedbackItem feedbackItem = feedbackCategoryAdapter.getFeedbackItem(feedbackPosition);
      onFeedbackSelected(feedbackItem);
    }
  };

  private void onFeedbackSelected(@NonNull FeedbackItem feedbackItem) {
    if (feedbackFlowType == FEEDBACK_MAIN_FLOW
        || feedbackItem.getFeedbackType().equals(FeedbackEvent.POSITIONING_ISSUE)) {
      if (feedbackBottomSheetListener != null) {
        feedbackBottomSheetListener.onFeedbackSelected(feedbackItem);
      }
      startTimer();
    } else {
      cancelCountdownAnimation();
      launchDetailFlow(feedbackItem);
    }
  }

  private void launchDetailFlow(@NonNull FeedbackItem feedbackItem) {
    feedbackSubTypeMap = buildFeedbackSubTypeMap();
    selectedFeedbackItem = feedbackItem;

    feedbackBottomSheetTitleText.setText(feedbackItem.getFeedbackText().replace('\n', ' '));
    initFeedbackIssueDetailRecyclerView(feedbackItem);
    feedbackProgressBar.setVisibility(View.GONE);
    feedbackMainLayout.setVisibility(View.GONE);
    feedbackSubTypesLayout.setVisibility(View.VISIBLE);
  }

  private void initFeedbackIssueDetailRecyclerView(@NonNull FeedbackItem feedbackItem) {
    feedbackSubTypeAdapter = new FeedbackSubTypeAdapter(descriptionItemClickListener);
    feedbackSubTypeAdapter.submitList(feedbackSubTypeMap.get(feedbackItem.getFeedbackType()));
    feedbackSubTypes.setAdapter(feedbackSubTypeAdapter);
    feedbackSubTypes.setOverScrollMode(RecyclerView.OVER_SCROLL_ALWAYS);
    feedbackSubTypes.setLayoutManager(new LinearLayoutManager(this.getContext()));
  }

  @NonNull
  private FeedbackSubTypeAdapter.OnSubTypeItemClickListener descriptionItemClickListener =
    new FeedbackSubTypeAdapter.OnSubTypeItemClickListener() {
      @Override
      public boolean onItemClick(int position) {
        FeedbackSubTypeItem item = feedbackSubTypeAdapter.getFeedbackSubTypeItem(position);
        if (selectedFeedbackItem.getFeedbackSubType().add(item.getFeedbackDescription())) {
          item.setChecked(true);
          return true;
        } else {
          selectedFeedbackItem.getFeedbackSubType().remove(item.getFeedbackDescription());
          item.setChecked(false);
          return false;
        }
      }
    };

  @NonNull
  private Map<String, List<FeedbackSubTypeItem>> buildFeedbackSubTypeMap() {
    final Map<String, List<FeedbackSubTypeItem>> map = new HashMap<>();

    map.put(FeedbackEvent.INCORRECT_VISUAL_GUIDANCE, subTypeOfIncorrectVisualGuidance());
    map.put(FeedbackEvent.INCORRECT_AUDIO_GUIDANCE, subTypeOfIncorrectAudioGuidance());
    map.put(FeedbackEvent.ROUTING_ERROR, subTypeOfRoutingError());
    map.put(FeedbackEvent.NOT_ALLOWED, subTypeOfNotAllowed());
    map.put(FeedbackEvent.ROAD_CLOSED, subTypeOfRoadClosed());

    return map;
  }

  @NonNull
  private List<FeedbackSubTypeItem> subTypeOfIncorrectVisualGuidance() {
    List<FeedbackSubTypeItem> list = new ArrayList<>();

    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.TURN_ICON_INCORRECT,
      R.string.mapbox_feedback_description_turn_icon_incorrect));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.STREET_NAME_INCORRECT,
      R.string.mapbox_feedback_description_street_name_incorrect));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.INSTRUCTION_UNNECESSARY,
      R.string.mapbox_feedback_description_instruction_unnecessary));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.INSTRUCTION_MISSING,
      R.string.mapbox_feedback_description_instruction_missing));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.MANEUVER_INCORRECT,
      R.string.mapbox_feedback_description_maneuver_incorrect));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.EXIT_INFO_INCORRECT,
      R.string.mapbox_feedback_description_exit_info_incorrect));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.LANE_GUIDANCE_INCORRECT,
      R.string.mapbox_feedback_description_lane_guidance_incorrect));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.ROAD_KNOW_BY_DIFFERENT_NAME,
      R.string.mapbox_feedback_description_road_known_by_different_name));

    return list;
  }

  @NonNull
  private List<FeedbackSubTypeItem> subTypeOfIncorrectAudioGuidance() {
    List<FeedbackSubTypeItem> list = new ArrayList<>();

    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.GUIDANCE_TOO_EARLY,
      R.string.mapbox_feedback_description_guidance_too_early));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.GUIDANCE_TOO_LATE,
      R.string.mapbox_feedback_description_guidance_too_late));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.PRONUNCIATION_INCORRECT,
      R.string.mapbox_feedback_description_pronunciation_incorrect));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.ROAD_NAME_REPEATED,
      R.string.mapbox_feedback_description_road_name_repeated));

    return list;
  }

  @NonNull
  private List<FeedbackSubTypeItem> subTypeOfRoutingError() {
    List<FeedbackSubTypeItem> list = new ArrayList<>();

    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.ROUTE_NOT_DRIVE_ABLE,
      R.string.mapbox_feedback_description_route_not_drive_able));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.ROUTE_NOT_PREFERRED,
      R.string.mapbox_feedback_description_route_not_preferred));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.ALTERNATIVE_ROUTE_NOT_EXPECTED,
      R.string.mapbox_feedback_description_alternative_route_not_expected));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.ROUTE_INCLUDED_MISSING_ROADS,
      R.string.mapbox_feedback_description_route_included_missing_roads));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.ROUTE_HAD_ROADS_TOO_NARROW_TO_PASS,
      R.string.mapbox_feedback_description_route_had_roads_too_narrow_to_pass));

    return list;
  }

  @NonNull
  private List<FeedbackSubTypeItem> subTypeOfNotAllowed() {
    List<FeedbackSubTypeItem> list = new ArrayList<>();

    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.ROUTED_DOWN_A_ONE_WAY,
      R.string.mapbox_feedback_description_routed_down_a_one_way));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.TURN_WAS_NOT_ALLOWED,
      R.string.mapbox_feedback_description_turn_was_not_allowed));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.CARS_NOT_ALLOWED_ON_STREET,
      R.string.mapbox_feedback_description_cars_not_allowed_on_street));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.TURN_AT_INTERSECTION_WAS_UNPROTECTED,
      R.string.mapbox_feedback_description_turn_at_intersection_was_unprotected));

    return list;
  }

  @NonNull
  private List<FeedbackSubTypeItem> subTypeOfRoadClosed() {
    List<FeedbackSubTypeItem> list = new ArrayList<>();

    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.STREET_PERMANENTLY_BLOCKED_OFF,
      R.string.mapbox_feedback_description_street_permanently_blocked_off));
    list.add(new FeedbackSubTypeItem(
      FeedbackEvent.ROAD_IS_MISSING_FROM_MAP,
      R.string.mapbox_feedback_description_road_is_missing_from_map));

    return list;
  }

  @IntDef( {FEEDBACK_MAIN_FLOW, FEEDBACK_DETAIL_FLOW})
  @Retention(RetentionPolicy.SOURCE)
  public @interface FeedbackFlowType {
  }

  /**
   * FEEDBACK_MAIN_FLOW limits the feedback process during
   * turn-by-turn navigation to only one section. This section
   * provides a way for a user to select the {@link FeedbackItem}'s
   * high-level feedback type. A second section that asks for more detailed
   * information within the selected type, will NOT be shown if
   * FEEDBACK_MAIN_FLOW is used.
   */
  public static final int FEEDBACK_MAIN_FLOW = 0;

  /**
   * FEEDBACK_MAIN_FLOW limits the feedback process during
   * turn-by-turn navigation to only one section. This section
   * provides a way for a user to select the {@link FeedbackItem}'s
   * high-level feedback type. A second section that asks for more detailed
   * information within the selected type, WILL be shown if
   * FEEDBACK_DETAIL_FLOW is used.
   */
  public static final int FEEDBACK_DETAIL_FLOW = 1;

  private interface DismissCommand {
    void invoke();
  }

  private DismissCommand delayedDismissCommand = FeedbackBottomSheet.this::dismiss;
}
