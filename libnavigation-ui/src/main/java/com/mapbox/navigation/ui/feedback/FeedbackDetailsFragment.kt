package com.mapbox.navigation.ui.feedback

import android.annotation.SuppressLint
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.internal.utils.ViewUtils
import kotlinx.android.synthetic.main.mapbox_edit_text_feedback_optional_comment.*
import kotlinx.android.synthetic.main.mapbox_feedback_details_bottom_sheet.*
import kotlinx.android.synthetic.main.mapbox_feedback_details_fragment.*
import kotlinx.android.synthetic.main.mapbox_partial_feedback_bottom_sheet_top_banner.*
import kotlinx.android.synthetic.main.mapbox_partial_feedback_details_button_bar.*
import timber.log.Timber

class FeedbackDetailsFragment : Fragment() {

    private val feedbackSubTypeMap by lazy { buildFeedbackSubTypeMap() }
    private var feedbackItem: FeedbackItem? = null
        set(value) {
            field = value
            value?.let {
                screenshotView.setImageBitmap(ViewUtils.decodeScreenshot(screenshot))
                feedbackBottomSheetTitleText.text = buildTitleText(it.feedbackText.replace("\n", " "), currentFeedbackIndex, feedbackList.size)
                feedbackSubTypeAdapter.submitList(feedbackSubTypeMap[it.feedbackType])
                feedbackCommentEditText.setText(it.description)
                feedbackOptionalCommentLayout.visibility = if (it.description.isEmpty()) GONE else VISIBLE
                feedbackCategoryImage.setImageDrawable(AppCompatResources.getDrawable(context!!, it.feedbackImageId))
                feedbackCategoryImage.visibility = VISIBLE
            }
        }
    private lateinit var feedbackSubTypeAdapter: FeedbackSubTypeAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private val feedbackList = ArrayList<FeedbackItem>()
    private lateinit var screenshot: String
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
                    text = if (value == feedbackList.size - 1) getString(R.string.mapbox_feedback_details_flow_done) else getString(R.string.mapbox_feedback_details_flow_next)
                }

                feedbackItem = feedbackList[value]
            }
        }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.mapbox_feedback_details_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
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
        feedbackDetailsFlowStartButton.setOnClickListener {
            currentFeedbackIndex = 0
        }

        feedbackDetailsFlowBackButton.setOnClickListener {
            saveComment()
            currentFeedbackIndex--
        }

        feedbackDetailsFlowNextButton.setOnClickListener {
            saveComment()
            if (currentFeedbackIndex == feedbackList.size - 1) {
                Toast.makeText(context, "This is the last one", LENGTH_LONG).show()
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
        feedbackSubTypes.adapter = feedbackSubTypeAdapter
        feedbackSubTypes.overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
        feedbackSubTypes.layoutManager = LinearLayoutManager(context)
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
        fun newInstance(feedbacks: ArrayList<FeedbackItem>): FeedbackDetailsFragment =
            FeedbackDetailsFragment().apply {
                feedbackList.addAll(feedbacks)
                screenshot = """
            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
            xJXyqyi1glbhJWd/DWyt9OMo1Fz5pmUqbg7xzXLkaVwVdpntf2jLZtkrwoKkruco4t7JaxS4pfms
            73aV0bbLXW07PCso4cSeV7rJ2unxWWT4qxMNl2V7PDZZ7NSns8PdhOKkl25/M+fsu2/h0a0todW0
            JVdqpRSw7PBRbUbLNSTsrau0uWQfUIauuRSlVdWM3uasJQV3CSWK3CzTaenBmrVmQZaZMGjV9SjV
            u4K6M1s/evmZG1To0YruRic+QgADm0AAAAc9DbIbRNRpwqtNtKbg8OXb4AdAK05qrThUhnGcVJdz
            Vy1gOKlRq/8AktqqYqlOD3dmoq08u1P0KOVaW3wcYVYJVcLXTcXGzzv7ttMj6FhZgfO2WnXqVKH3
            h11FbOnK8pR6d+PbYmWNU4093VS3tVyko1Muk2so2ve+uh3qSc3C/SSTa7He3yZNmB850q9TZnKT
            2hVI7JBxSlJXqWlfJavQttEJS2p3jtDl94puOFTwKKw3/jrc77PkY1tphRmoNTnNq+GEXJ255cAM
            KLqw22StUqQm5NykpxUM7pZ5PkrHaE8STV7NXzVisJqpFyjopOPim0/kBYAiUlGUIy1m8K77N/JM
            CQAAAAAAACtT3GWKVH0O8M5emQAMvOAAAWhByTd0ktWyVGKgpt3d/dKzm5u78EuBvUntda9plJYV
            GMbc3xZUAzbst2AAiAAAEqTi8iC0I4nnoWb34Wf6awk30ldM6IVVLJ5P5mAPTHZvOkpZxyfzOatG
            pKKUakqU4vFFrS/auK7PKzszqpNuF278i0oqSs1co+JunsVJOu8ezbNesowSjGpUbeGKjd4VHK13
            q0+BelOGwUaka20U69aN621Yal508ld4dXFLxslqfRqUXZq2OLVmmuBwVtgVSpiU3KCqb77vO2CU
            +GdrxzzevcB2yi4PNeJByfZ9OM9urSVKrDPG5zg4Sk5XvCTeU1HKzV0lZd+G07TGW2129o2jZqNG
            1OnOlTc4Sn+a+TTzcY2ed7pZgfW2meGMe85972epttmkPE5ThyXyxcrL4ab3s9RvVyMwY2netd6u
            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
            AAAAAAAAAAAAAAAAAP/Z
            """.trimIndent()
            }
    }
}
