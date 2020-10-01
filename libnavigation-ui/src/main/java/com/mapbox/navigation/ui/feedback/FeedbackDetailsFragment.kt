package com.mapbox.navigation.ui.feedback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent.POSITIONING_ISSUE
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.internal.utils.ViewUtils
import kotlinx.android.synthetic.main.mapbox_edit_text_feedback_optional_comment.feedbackCommentEditText
import kotlinx.android.synthetic.main.mapbox_feedback_details_bottom_sheet.addMoreFeedbackCommentsTextView
import kotlinx.android.synthetic.main.mapbox_feedback_details_bottom_sheet.feedbackDetailsBottomSheet
import kotlinx.android.synthetic.main.mapbox_feedback_details_bottom_sheet.feedbackDetailsBottomSheetLayout
import kotlinx.android.synthetic.main.mapbox_feedback_details_bottom_sheet.feedbackOptionalCommentLayout
import kotlinx.android.synthetic.main.mapbox_feedback_details_bottom_sheet.feedbackSubTypes
import kotlinx.android.synthetic.main.mapbox_feedback_details_bottom_sheet.provideFeedbackDetailTextView
import kotlinx.android.synthetic.main.mapbox_feedback_details_fragment.screenshotView
import kotlinx.android.synthetic.main.mapbox_partial_feedback_bottom_sheet_top_banner.cancelBtn
import kotlinx.android.synthetic.main.mapbox_partial_feedback_bottom_sheet_top_banner.feedbackBottomSheetTitleText
import kotlinx.android.synthetic.main.mapbox_partial_feedback_bottom_sheet_top_banner.feedbackCategoryImage
import kotlinx.android.synthetic.main.mapbox_partial_feedback_details_button_bar.feedbackDetailsFlowBackButton
import kotlinx.android.synthetic.main.mapbox_partial_feedback_details_button_bar.feedbackDetailsFlowNextButton
import kotlinx.android.synthetic.main.mapbox_partial_feedback_details_button_bar.feedbackDetailsFlowStartButton
import timber.log.Timber

/**
 * This [Fragment] is responsible for showing UI related to providing
 * more specific information about reported feedback topics.
 */
class FeedbackDetailsFragment : DialogFragment() {

    private val feedbackSubTypeMap by lazy { buildFeedbackSubTypeMap() }
    private var feedbackItem: FeedbackItem? = null
        set(value) {
            field = value
            value?.let {
                screenshotView.setImageBitmap(ViewUtils.decodeScreenshot(it.encodedScreenshot))
                feedbackBottomSheetTitleText.text = buildTitleText(it.feedbackText.replace("\n", " "), currentFeedbackIndex, itemsToProvideMoreDetailsOn.size)
                /**
                 * TODO: Show something else if it.feedbackType == positioning_issue and/or
                 *  feedbackSubTypeMap[it.feedbackType] = null ?
                 */
                feedbackSubTypeAdapter.submitList(feedbackSubTypeMap[it.feedbackType])
                feedbackCommentEditText.setText(it.description)
                feedbackOptionalCommentLayout.visibility = if (it.description.isEmpty()) GONE else VISIBLE
                feedbackCategoryImage.setImageDrawable(AppCompatResources.getDrawable(context!!, it.feedbackImageId))
                feedbackCategoryImage.visibility = VISIBLE
            }
        }
    private lateinit var feedbackSubTypeAdapter: FeedbackSubTypeAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var feedbackFlowListener: FeedbackFlowListener? = null
    private var arrivalExperienceFeedbackEnabled: Boolean = false
    private val itemsToProvideMoreDetailsOn = ArrayList<FeedbackItem>()
    private val itemsThatDontNeedDetailedFeedback = ArrayList<FeedbackItem>()
    private var currentFeedbackIndex = -1
        set(value) {
            field = value

            if (value == -1) {
                bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(R.dimen.mapbox_feedback_details_bottom_sheet_initial_peek_height)
                provideFeedbackDetailTextView.visibility = VISIBLE
                feedbackDetailsBottomSheetLayout.visibility = GONE

                feedbackDetailsFlowStartButton.visibility = VISIBLE
                feedbackDetailsFlowBackButton.visibility = GONE
                feedbackDetailsFlowNextButton.visibility = GONE
            } else {
                bottomSheetBehavior.peekHeight = resources.getDimensionPixelSize(R.dimen.mapbox_feedback_details_bottom_sheet_peek_height)
                provideFeedbackDetailTextView.visibility = GONE
                feedbackDetailsBottomSheetLayout.visibility = VISIBLE

                feedbackDetailsFlowStartButton.visibility = GONE
                feedbackDetailsFlowBackButton.apply {
                    visibility = VISIBLE
                    isEnabled = value > 0
                }
                feedbackDetailsFlowNextButton.apply {
                    visibility = VISIBLE
                    text = if (value == itemsToProvideMoreDetailsOn.size - 1) getString(R.string.mapbox_feedback_details_flow_done) else getString(R.string.mapbox_feedback_details_flow_next)
                }

                if (itemsToProvideMoreDetailsOn.isNotEmpty()) {
                    feedbackItem = itemsToProvideMoreDetailsOn[value]
                }
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.mapbox_feedback_details_fragment, container, false)
    }

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
            dismiss()
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
                feedbackFlowListener?.onDetailedFeedbackFlowFinished()
                if (arrivalExperienceFeedbackEnabled) {
                    goToArrivalExperienceFragment()
                }
                dismiss()
            } else {
                currentFeedbackIndex++
            }
        }

        addMoreFeedbackCommentsTextView.setOnClickListener { feedbackOptionalCommentLayout.visibility = VISIBLE }

        feedbackCommentEditText.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    private fun goToArrivalExperienceFragment() {
        val fragmentActivityContext = context as FragmentActivity
        fragmentActivityContext.supportFragmentManager
            .beginTransaction()
            .add(R.id.navigationLayout, FeedbackArrivalFragment.newInstance(
                feedbackFlowListener, itemsToProvideMoreDetailsOn.plus(itemsThatDontNeedDetailedFeedback)), FeedbackArrivalFragment::class.java.simpleName).commit()
    }

    private fun initRecyclerView() {
        feedbackSubTypeAdapter = FeedbackSubTypeAdapter(object : FeedbackSubTypeAdapter.OnSubTypeItemClickListener {
            override fun onItemClick(position: Int): Boolean {
                feedbackItem?.let {
                    val feedbackSubTypeItem = feedbackSubTypeAdapter.getFeedbackSubTypeItem(position)
                    return if (it.feedbackSubType.add(feedbackSubTypeItem.feedbackDescription)) {
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
        })
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

    private fun buildTitleText(feedbackCategory: String = "", currentFeedbackIndex: Int, totalFeedbackNumber: Int): String =
        if (currentFeedbackIndex == -1) {
            resources.getString(R.string.mapbox_feedback_details_flow_title)
        } else {
            "$feedbackCategory (${currentFeedbackIndex + 1}/$totalFeedbackNumber)"
        }

    private fun buildFeedbackSubTypeMap(): Map<String, List<FeedbackSubTypeItem>> {
        val map = mutableMapOf<String, List<FeedbackSubTypeItem>>().withDefault { emptyList() }
        map[FeedbackEvent.INCORRECT_VISUAL_GUIDANCE] = subTypeOfIncorrectVisualGuidance()
        map[FeedbackEvent.INCORRECT_AUDIO_GUIDANCE] = subTypeOfIncorrectAudioGuidance()
        map[FeedbackEvent.ROUTING_ERROR] = subTypeOfRoutingError()
        map[FeedbackEvent.NOT_ALLOWED] = subTypeOfNotAllowed()
        map[FeedbackEvent.ROAD_CLOSED] = subTypeOfRoadClosed()
        return map
    }

    private fun subTypeOfIncorrectVisualGuidance(): List<FeedbackSubTypeItem> {
        val list: MutableList<FeedbackSubTypeItem> = ArrayList()
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.TURN_ICON_INCORRECT,
            R.string.mapbox_feedback_description_turn_icon_incorrect))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.STREET_NAME_INCORRECT,
            R.string.mapbox_feedback_description_street_name_incorrect))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.INSTRUCTION_UNNECESSARY,
            R.string.mapbox_feedback_description_instruction_unnecessary))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.INSTRUCTION_MISSING,
            R.string.mapbox_feedback_description_instruction_missing))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.MANEUVER_INCORRECT,
            R.string.mapbox_feedback_description_maneuver_incorrect))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.EXIT_INFO_INCORRECT,
            R.string.mapbox_feedback_description_exit_info_incorrect))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.LANE_GUIDANCE_INCORRECT,
            R.string.mapbox_feedback_description_lane_guidance_incorrect))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.ROAD_KNOW_BY_DIFFERENT_NAME,
            R.string.mapbox_feedback_description_road_known_by_different_name))
        return list
    }

    private fun subTypeOfIncorrectAudioGuidance(): List<FeedbackSubTypeItem> {
        val list: MutableList<FeedbackSubTypeItem> = ArrayList()
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.GUIDANCE_TOO_EARLY,
            R.string.mapbox_feedback_description_guidance_too_early))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.GUIDANCE_TOO_LATE,
            R.string.mapbox_feedback_description_guidance_too_late))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.PRONUNCIATION_INCORRECT,
            R.string.mapbox_feedback_description_pronunciation_incorrect))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.ROAD_NAME_REPEATED,
            R.string.mapbox_feedback_description_road_name_repeated))
        return list
    }

    private fun subTypeOfRoutingError(): List<FeedbackSubTypeItem> {
        val list: MutableList<FeedbackSubTypeItem> = ArrayList()
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.ROUTE_NOT_DRIVE_ABLE,
            R.string.mapbox_feedback_description_route_not_drive_able))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.ROUTE_NOT_PREFERRED,
            R.string.mapbox_feedback_description_route_not_preferred))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.ALTERNATIVE_ROUTE_NOT_EXPECTED,
            R.string.mapbox_feedback_description_alternative_route_not_expected))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.ROUTE_INCLUDED_MISSING_ROADS,
            R.string.mapbox_feedback_description_route_included_missing_roads))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.ROUTE_HAD_ROADS_TOO_NARROW_TO_PASS,
            R.string.mapbox_feedback_description_route_had_roads_too_narrow_to_pass))
        return list
    }

    private fun subTypeOfNotAllowed(): List<FeedbackSubTypeItem> {
        val list: MutableList<FeedbackSubTypeItem> = ArrayList()
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.ROUTED_DOWN_A_ONE_WAY,
            R.string.mapbox_feedback_description_routed_down_a_one_way))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.TURN_WAS_NOT_ALLOWED,
            R.string.mapbox_feedback_description_turn_was_not_allowed))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.CARS_NOT_ALLOWED_ON_STREET,
            R.string.mapbox_feedback_description_cars_not_allowed_on_street))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.TURN_AT_INTERSECTION_WAS_UNPROTECTED,
            R.string.mapbox_feedback_description_turn_at_intersection_was_unprotected))
        return list
    }

    private fun subTypeOfRoadClosed(): List<FeedbackSubTypeItem> {
        val list: MutableList<FeedbackSubTypeItem> = ArrayList()
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.STREET_PERMANENTLY_BLOCKED_OFF,
            R.string.mapbox_feedback_description_street_permanently_blocked_off))
        list.add(FeedbackSubTypeItem(
            FeedbackEvent.ROAD_IS_MISSING_FROM_MAP,
            R.string.mapbox_feedback_description_road_is_missing_from_map))
        return list
    }

    private val bottomSheetCallBack = object : BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            Timber.e("DaiJun slideOffset: $slideOffset")
            hideSoftInput(bottomSheet)
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
            Timber.e("DaiJun newState: $newState")
        }
    }

    companion object {
        fun newInstance(enableArrivalExperienceFeedback: Boolean, feedbackFlowListener: FeedbackFlowListener?, feedbackItemList: List<FeedbackItem>?): FeedbackDetailsFragment =
            FeedbackDetailsFragment().apply {
                this.arrivalExperienceFeedbackEnabled = enableArrivalExperienceFeedback
                this.feedbackFlowListener = feedbackFlowListener
                feedbackItemList?.let {
                    val itemsThatNeedMoreDetail = it.filter { singleItem ->
                        !singleItem.feedbackType.contentEquals(POSITIONING_ISSUE)
                    }
                    itemsToProvideMoreDetailsOn.addAll(itemsThatNeedMoreDetail)

                    val itemsThatDoNotNeedDetail = it.filter { singleItem ->
                        singleItem.feedbackType.contentEquals(POSITIONING_ISSUE)
                    }
                    itemsThatDontNeedDetailedFeedback.addAll(itemsThatDoNotNeedDetail)
                }
            }

        fun newInstance(feedbackFlowListener: FeedbackFlowListener?, feedbackItemList: List<FeedbackItem>?): FeedbackDetailsFragment =
            newInstance(false, feedbackFlowListener, feedbackItemList)
    }
}
