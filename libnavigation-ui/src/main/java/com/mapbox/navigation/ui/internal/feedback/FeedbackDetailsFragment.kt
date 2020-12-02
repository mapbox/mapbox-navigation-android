package com.mapbox.navigation.ui.internal.feedback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_COLLAPSED
import com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_HIDDEN
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.mapbox.navigation.core.internal.telemetry.CachedNavigationFeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent.POSITIONING_ISSUE
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.feedback.FeedbackHelper
import com.mapbox.navigation.ui.feedback.FeedbackSubTypeAdapter
import com.mapbox.navigation.ui.feedback.FeedbackSubTypeItem
import com.mapbox.navigation.ui.internal.utils.ViewUtils
import kotlinx.android.synthetic.main.mapbox_edit_text_feedback_optional_comment.*
import kotlinx.android.synthetic.main.mapbox_feedback_details_bottom_sheet.*
import kotlinx.android.synthetic.main.mapbox_feedback_details_fragment.*
import kotlinx.android.synthetic.main.mapbox_partial_feedback_bottom_sheet_top_banner.*
import kotlinx.android.synthetic.main.mapbox_partial_feedback_details_button_bar.*

/**
 * This [Fragment] is responsible for showing UI related to providing
 * more specific information about reported feedback topics.
 */
class FeedbackDetailsFragment : DialogFragment() {

    private var feedbackItem: CachedNavigationFeedbackEvent? = null
        set(value) {
            field = value
            value?.let {
                context?.let { context ->
                    feedbackDetailsRoot.setBackgroundColor(
                        ContextCompat.getColor(
                            context,
                            R.color.mapbox_feedback_bottom_sheet_background
                        )
                    )
                }
                screenshotView.setImageBitmap(ViewUtils.decodeScreenshot(it.screenshot))
                feedbackBottomSheetTitleText.text = buildTitleText(
                    FeedbackHelper.getFeedbackText(it.feedbackType, requireContext())
                        .replace("\n", " "),
                    currentFeedbackIndex,
                    itemsToProvideMoreDetailsOn.size
                )
                feedbackSubTypeAdapter.submitList(getFeedbackSubTypes(it))
                feedbackCommentEditText.setText(it.description)
                feedbackOptionalCommentLayout.visibility =
                    if (it.description?.isEmpty() == true) GONE else VISIBLE
                feedbackCategoryImage.setImageDrawable(
                    AppCompatResources.getDrawable(
                        context!!,
                        FeedbackHelper.getFeedbackImage(it.feedbackType)
                    )
                )
                feedbackCategoryImage.visibility = VISIBLE
            }
        }
    private lateinit var feedbackSubTypeAdapter: FeedbackSubTypeAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var feedbackFlowListener: FeedbackFlowListener
    private var arrivalExperienceFeedbackEnabled: Boolean = false
    private val itemsToProvideMoreDetailsOn = ArrayList<CachedNavigationFeedbackEvent>()
    private val itemsThatDontNeedDetailedFeedback = ArrayList<CachedNavigationFeedbackEvent>()
    private var currentFeedbackIndex = -1
        set(value) {
            field = value

            if (value == -1) {
                bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(
                    R.dimen.mapbox_feedback_details_bottom_sheet_initial_peek_height
                )
                provideFeedbackDetailTextView.visibility = VISIBLE
                feedbackDetailsBottomSheetLayout.visibility = GONE

                feedbackDetailsFlowStartButton.visibility = VISIBLE
                feedbackDetailsFlowBackButton.visibility = GONE
                feedbackDetailsFlowNextButton.visibility = GONE
            } else {
                bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(
                    R.dimen.mapbox_feedback_details_bottom_sheet_peek_height
                )
                provideFeedbackDetailTextView.visibility = GONE
                feedbackDetailsBottomSheetLayout.visibility = VISIBLE

                feedbackDetailsFlowStartButton.visibility = GONE
                feedbackDetailsFlowBackButton.apply {
                    visibility = VISIBLE
                    isEnabled = value > 0
                }
                feedbackDetailsFlowNextButton.apply {
                    visibility = VISIBLE
                    text = if (value == itemsToProvideMoreDetailsOn.size - 1) getString(
                        R.string.mapbox_feedback_details_flow_done
                    ) else getString(R.string.mapbox_feedback_details_flow_next)
                }

                if (itemsToProvideMoreDetailsOn.isNotEmpty()) {
                    feedbackItem = itemsToProvideMoreDetailsOn[value]
                }
            }
        }

    /**
     * Called to have the fragment instantiate its user interface view.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to. The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return Return the View for the fragment's UI, or null.
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.mapbox_feedback_details_fragment, container, false)
    }

    /**
     * Called immediately after [onCreateView] has returned, but before any saved state
     * has been restored in to the view.
     *
     * @param view The View returned by [onCreateView].
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cancelBtn.setColorFilter(Color.WHITE)
        initBottomSheetBehavior()
        initTitleTextView()
        initListeners()
        initRecyclerView()
    }

    private fun initTitleTextView() {
        feedbackBottomSheetTitleText.setText(R.string.mapbox_feedback_details_flow_title)
        feedbackCategoryImage.visibility = GONE
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners() {
        cancelBtn.setOnClickListener {
            finishDetailsFlow()
        }
        feedbackDetailsFlowStartButton.setOnClickListener {
            currentFeedbackIndex = 0
        }

        feedbackDetailsFlowBackButton.setOnClickListener {
            saveComment()
            currentFeedbackIndex--
        }

        feedbackDetailsFlowNextButton.setOnClickListener {
            saveComment()
            if (currentFeedbackIndex == itemsToProvideMoreDetailsOn.size - 1) {
                finishDetailsFlow()
            } else {
                currentFeedbackIndex++
            }
        }

        addMoreFeedbackCommentsTextView.setOnClickListener {
            feedbackOptionalCommentLayout.visibility = VISIBLE
        }

        feedbackCommentEditText.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }

        screenshotView.setOnClickListener {
            if (currentFeedbackIndex >= 0) {
                if (buttonLayout.visibility == VISIBLE) {
                    buttonLayout.visibility = INVISIBLE
                    bottomSheetBehavior.isHideable = true
                    bottomSheetBehavior.state = STATE_HIDDEN
                } else {
                    buttonLayout.visibility = VISIBLE
                    bottomSheetBehavior.isHideable = false
                    bottomSheetBehavior.state = STATE_COLLAPSED
                }
            }
        }
    }

    private fun finishDetailsFlow() {
        feedbackFlowListener.onDetailedFeedbackFlowFinished(
            itemsToProvideMoreDetailsOn.plus(itemsThatDontNeedDetailedFeedback)
        )
        if (arrivalExperienceFeedbackEnabled) {
            goToArrivalExperienceFragment()
        }
        dismiss()
    }

    private fun goToArrivalExperienceFragment() {
        parentFragmentManager.beginTransaction()
            .add(
                android.R.id.content,
                FeedbackArrivalFragment.newInstance(
                    feedbackFlowListener
                ),
                FeedbackArrivalFragment::class.java.simpleName
            ).commit()
    }

    private fun initRecyclerView() {
        feedbackSubTypeAdapter = FeedbackSubTypeAdapter(
            object : FeedbackSubTypeAdapter.OnSubTypeItemClickListener {
                override fun onItemClick(position: Int): Boolean {
                    feedbackItem?.let {
                        val feedbackSubTypeItem = feedbackSubTypeAdapter.getFeedbackSubTypeItem(
                            position
                        )
                        return if (
                            it.feedbackSubType.add(feedbackSubTypeItem.feedbackDescription)
                        ) {
                            feedbackSubTypeItem.isChecked = true
                            true
                        } else {
                            it.feedbackSubType.remove(feedbackSubTypeItem.feedbackDescription)
                            feedbackSubTypeItem.isChecked = false
                            false
                        }
                    }
                    return false
                }
            }
        )
        feedbackSubTypes?.apply {
            adapter = feedbackSubTypeAdapter
            overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun initBottomSheetBehavior() {
        bottomSheetBehavior = from(feedbackDetailsBottomSheet)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallBack)
    }

    private fun saveComment() {
        feedbackItem?.apply {
            description = feedbackCommentEditText.text.toString()
        }

        hideSoftInput(feedbackCommentEditText)
    }

    private fun hideSoftInput(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    private fun buildTitleText(
        feedbackCategory: String = "",
        currentFeedbackIndex: Int,
        totalFeedbackNumber: Int
    ): String =
        if (currentFeedbackIndex == -1) {
            resources.getString(R.string.mapbox_feedback_details_flow_title)
        } else {
            "$feedbackCategory (${currentFeedbackIndex + 1}/$totalFeedbackNumber)"
        }

    private fun getFeedbackSubTypes(
        feedbackItem: CachedNavigationFeedbackEvent
    ): List<FeedbackSubTypeItem> {
        return when (feedbackItem.feedbackType) {
            FeedbackEvent.INCORRECT_VISUAL_GUIDANCE -> subTypeOfIncorrectVisualGuidance()
            FeedbackEvent.INCORRECT_AUDIO_GUIDANCE -> subTypeOfIncorrectAudioGuidance()
            FeedbackEvent.ROUTING_ERROR -> subTypeOfRoutingError()
            FeedbackEvent.NOT_ALLOWED -> subTypeOfNotAllowed()
            FeedbackEvent.ROAD_CLOSED -> subTypeOfRoadClosed()
            else -> emptyList()
        }.apply {
            forEach { it.isChecked = it.feedbackDescription in feedbackItem.feedbackSubType }
        }
    }

    private fun subTypeOfIncorrectVisualGuidance(): List<FeedbackSubTypeItem> {
        val list: MutableList<FeedbackSubTypeItem> = ArrayList()
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.TURN_ICON_INCORRECT,
                R.string.mapbox_feedback_description_turn_icon_incorrect
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.STREET_NAME_INCORRECT,
                R.string.mapbox_feedback_description_street_name_incorrect
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.INSTRUCTION_UNNECESSARY,
                R.string.mapbox_feedback_description_instruction_unnecessary
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.INSTRUCTION_MISSING,
                R.string.mapbox_feedback_description_instruction_missing
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.MANEUVER_INCORRECT,
                R.string.mapbox_feedback_description_maneuver_incorrect
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.EXIT_INFO_INCORRECT,
                R.string.mapbox_feedback_description_exit_info_incorrect
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.LANE_GUIDANCE_INCORRECT,
                R.string.mapbox_feedback_description_lane_guidance_incorrect
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.ROAD_KNOW_BY_DIFFERENT_NAME,
                R.string.mapbox_feedback_description_road_known_by_different_name
            )
        )
        return list
    }

    private fun subTypeOfIncorrectAudioGuidance(): List<FeedbackSubTypeItem> {
        val list: MutableList<FeedbackSubTypeItem> = ArrayList()
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.GUIDANCE_TOO_EARLY,
                R.string.mapbox_feedback_description_guidance_too_early
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.GUIDANCE_TOO_LATE,
                R.string.mapbox_feedback_description_guidance_too_late
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.PRONUNCIATION_INCORRECT,
                R.string.mapbox_feedback_description_pronunciation_incorrect
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.ROAD_NAME_REPEATED,
                R.string.mapbox_feedback_description_road_name_repeated
            )
        )
        return list
    }

    private fun subTypeOfRoutingError(): List<FeedbackSubTypeItem> {
        val list: MutableList<FeedbackSubTypeItem> = ArrayList()
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.ROUTE_NOT_DRIVE_ABLE,
                R.string.mapbox_feedback_description_route_not_drive_able
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.ROUTE_NOT_PREFERRED,
                R.string.mapbox_feedback_description_route_not_preferred
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.ALTERNATIVE_ROUTE_NOT_EXPECTED,
                R.string.mapbox_feedback_description_alternative_route_not_expected
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.ROUTE_INCLUDED_MISSING_ROADS,
                R.string.mapbox_feedback_description_route_included_missing_roads
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.ROUTE_HAD_ROADS_TOO_NARROW_TO_PASS,
                R.string.mapbox_feedback_description_route_had_roads_too_narrow_to_pass
            )
        )
        return list
    }

    private fun subTypeOfNotAllowed(): List<FeedbackSubTypeItem> {
        val list: MutableList<FeedbackSubTypeItem> = ArrayList()
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.ROUTED_DOWN_A_ONE_WAY,
                R.string.mapbox_feedback_description_routed_down_a_one_way
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.TURN_WAS_NOT_ALLOWED,
                R.string.mapbox_feedback_description_turn_was_not_allowed
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.CARS_NOT_ALLOWED_ON_STREET,
                R.string.mapbox_feedback_description_cars_not_allowed_on_street
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.TURN_AT_INTERSECTION_WAS_UNPROTECTED,
                R.string.mapbox_feedback_description_turn_at_intersection_was_unprotected
            )
        )
        return list
    }

    private fun subTypeOfRoadClosed(): List<FeedbackSubTypeItem> {
        val list: MutableList<FeedbackSubTypeItem> = ArrayList()
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.STREET_PERMANENTLY_BLOCKED_OFF,
                R.string.mapbox_feedback_description_street_permanently_blocked_off
            )
        )
        list.add(
            FeedbackSubTypeItem(
                FeedbackEvent.ROAD_IS_MISSING_FROM_MAP,
                R.string.mapbox_feedback_description_road_is_missing_from_map
            )
        )
        return list
    }

    private val bottomSheetCallBack = object : BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            hideSoftInput(bottomSheet)
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
        }
    }

    companion object {
        /**
         * Create a new instance of [FeedbackDetailsFragment] with given feedback items.
         *
         * @param feedbackItemList is a list of [CachedNavigationFeedbackEvent] which is generated by the user along with the route.
         *  In feedback detail mode, user can provide more details about a feedback. Like providing feedback sub types and feedback description.
         * @param feedbackFlowListener a listener will be called when the user is done with providing feedback details.
         *  This listener will be passed to [FeedbackArrivalFragment] as the callback when the user finishes the arrival feedback.
         * @param enableArrivalExperienceFeedback true to display a [FeedbackArrivalFragment] when the [FeedbackDetailsFragment] flow is done.
         */
        @JvmStatic
        fun newInstance(
            feedbackItemList: List<CachedNavigationFeedbackEvent>,
            feedbackFlowListener: FeedbackFlowListener,
            enableArrivalExperienceFeedback: Boolean = true
        ) =
            FeedbackDetailsFragment().apply {
                retainInstance = true
                this.arrivalExperienceFeedbackEnabled = enableArrivalExperienceFeedback
                this.feedbackFlowListener = feedbackFlowListener
                feedbackItemList.partition {
                    it.feedbackType == POSITIONING_ISSUE
                }.run {
                    itemsThatDontNeedDetailedFeedback.addAll(first)
                    itemsToProvideMoreDetailsOn.addAll(second)
                }
            }
    }
}
