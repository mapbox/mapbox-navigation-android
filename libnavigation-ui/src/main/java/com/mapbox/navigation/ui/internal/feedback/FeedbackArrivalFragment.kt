package com.mapbox.navigation.ui.internal.feedback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.feedback.FeedbackHelper
import com.mapbox.navigation.ui.feedback.FeedbackSubTypeAdapter
import com.mapbox.navigation.ui.feedback.FeedbackSubTypeItem
import kotlinx.android.synthetic.main.mapbox_edit_text_feedback_optional_comment.*
import kotlinx.android.synthetic.main.mapbox_feedback_arrival_bottom_sheet.*
import kotlinx.android.synthetic.main.mapbox_feedback_arrival_fragment.*
import kotlinx.android.synthetic.main.mapbox_item_feedback.view.*
import kotlinx.android.synthetic.main.mapbox_partial_feedback_arrival_button_bar.feedbackArrivalFinishButton
import kotlinx.android.synthetic.main.mapbox_partial_feedback_bottom_sheet_top_banner.cancelBtn
import kotlinx.android.synthetic.main.mapbox_partial_feedback_bottom_sheet_top_banner.feedbackBottomSheetTitleText

/**
 * This [Fragment] is responsible for showing UI related to providing
 * information.
 */
class FeedbackArrivalFragment : DialogFragment() {

    private lateinit var feedbackSubTypeAdapter: FeedbackSubTypeAdapter
    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private lateinit var feedbackFlowListener: FeedbackFlowListener
    private val feedbackSubType: MutableSet<String> = HashSet()

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
        return inflater.inflate(R.layout.mapbox_feedback_arrival_fragment, container, false)
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
        initBottomSheetBehavior()
        initView()
        initListeners()
    }

    private fun initView() {
        cancelBtn.setColorFilter(Color.WHITE)
        initTitleTextView()
        initFeedbackPositiveIcon()
        initFeedbackNegativeIcon()
        showOverallExperienceLayout()
    }

    private fun initFeedbackPositiveIcon() {
        arrivalFeedbackPositive.setBackgroundResource(android.R.color.transparent)
        arrivalFeedbackPositive.feedbackImage.setImageResource(
            R.drawable.mapbox_ic_feedback_overall_positive
        )
        arrivalFeedbackPositive.feedbackText.setText(
            R.string.mapbox_feedback_arrival_experience_good
        )
        arrivalFeedbackPositive.feedbackImage.setOnClickListener {
            sendPositiveFeedback()
            dismiss()
        }
    }

    private fun initFeedbackNegativeIcon() {
        arrivalFeedbackNegative.setBackgroundResource(android.R.color.transparent)
        arrivalFeedbackNegative.feedbackImage.setImageResource(
            R.drawable.mapbox_ic_feedback_overall_negative
        )
        arrivalFeedbackNegative.feedbackText.setText(
            R.string.mapbox_feedback_arrival_experience_not_good
        )
        arrivalFeedbackNegative.feedbackImage.setOnClickListener {
            showNegativeFeedbackLayout()
        }
    }

    private fun showOverallExperienceLayout() {
        arrivalOverallExperienceLayout.visibility = VISIBLE
        buttonLayout.visibility = GONE
        negativeFeedbackLayout.visibility = GONE
    }

    private fun showNegativeFeedbackLayout() {
        arrivalOverallExperienceLayout.visibility = GONE
        buttonLayout.visibility = VISIBLE
        negativeFeedbackLayout.visibility = VISIBLE
        initRecyclerView()
    }

    private fun initTitleTextView() {
        feedbackBottomSheetTitleText.setText(R.string.mapbox_feedback_arrival_feedback)
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initListeners() {
        cancelBtn.setOnClickListener {
            dismiss()
        }
        feedbackArrivalFinishButton.setOnClickListener {
            sendNegativeFeedback()
            hideSoftInput(feedbackCommentEditText)
            dismiss()
        }
        feedbackCommentEditText.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            if (event.actionMasked == MotionEvent.ACTION_UP) {
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    private fun initBottomSheetBehavior() {
        bottomSheetBehavior = from(feedbackArrivalBottomSheet)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallBack)
    }

    private fun initRecyclerView() {
        feedbackSubTypeAdapter = FeedbackSubTypeAdapter(
            object : FeedbackSubTypeAdapter.OnSubTypeItemClickListener {
                override fun onItemClick(position: Int): Boolean {
                    val feedbackSubTypeItem = feedbackSubTypeAdapter.getFeedbackSubTypeItem(
                        position
                    )
                    return if (
                        feedbackSubType.add(feedbackSubTypeItem.feedbackDescription)
                    ) {
                        feedbackSubTypeItem.isChecked = true
                        true
                    } else {
                        feedbackSubType.remove(feedbackSubTypeItem.feedbackDescription)
                        feedbackSubTypeItem.isChecked = false
                        false
                    }
                }
            }
        ).apply {
            submitList(
                listOf(
                    FeedbackSubTypeItem(
                        FeedbackEvent.ARRIVAL_FEEDBACK_WRONG_LOCATION,
                        R.string.mapbox_feedback_description_wrong_location
                    ),
                    FeedbackSubTypeItem(
                        FeedbackEvent.ARRIVAL_FEEDBACK_WRONG_ENTRANCE,
                        R.string.mapbox_feedback_description_wrong_entrance
                    ),
                    FeedbackSubTypeItem(
                        FeedbackEvent.ARRIVAL_FEEDBACK_CONFUSING_INSTRUCTIONS,
                        R.string.mapbox_feedback_description_confusing_instructions
                    ),
                    FeedbackSubTypeItem(
                        FeedbackEvent.ARRIVAL_FEEDBACK_THIS_PLACE_IS_CLOSED,
                        R.string.mapbox_feedback_description_this_place_is_closed
                    )
                )
            )
        }
        feedbackSubTypes.apply {
            adapter = feedbackSubTypeAdapter
            overScrollMode = RecyclerView.OVER_SCROLL_ALWAYS
            layoutManager = LinearLayoutManager(context)
        }
    }

    private fun sendPositiveFeedback() {
        FeedbackHelper.getArrivalFeedbackItem(
            getString(R.string.mapbox_feedback_type_general),
            PLACEHOLDER_INT_FOR_GENERAL_FEEDBACK_DRAWABLE,
            FeedbackEvent.ARRIVAL_FEEDBACK_GOOD,
            ""
        ).let {
            feedbackFlowListener.onArrivalExperienceFeedbackFinished(it)
        }
    }

    private fun sendNegativeFeedback() {
        FeedbackHelper.getArrivalFeedbackItem(
            getString(R.string.mapbox_feedback_type_general),
            PLACEHOLDER_INT_FOR_GENERAL_FEEDBACK_DRAWABLE,
            FeedbackEvent.ARRIVAL_FEEDBACK_NOT_GOOD,
            feedbackCommentEditText.text.toString()
        ).apply {
            feedbackSubType.addAll(this@FeedbackArrivalFragment.feedbackSubType)
        }.let {
            feedbackFlowListener.onArrivalExperienceFeedbackFinished(it)
        }
    }

    private fun hideSoftInput(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
    }

    private val bottomSheetCallBack = object : BottomSheetCallback() {
        override fun onSlide(bottomSheet: View, slideOffset: Float) {
            hideSoftInput(bottomSheet)
        }

        override fun onStateChanged(bottomSheet: View, newState: Int) {
        }
    }

    companion object {
        private const val PLACEHOLDER_INT_FOR_GENERAL_FEEDBACK_DRAWABLE = 0

        /**
         * Create a new instance of [FeedbackArrivalFragment] to submit feedback on arrival.
         *
         * @param feedbackFlowListener is called when the user finishes the arrival feedback.
         */
        @JvmStatic
        fun newInstance(feedbackFlowListener: FeedbackFlowListener): FeedbackArrivalFragment =
            FeedbackArrivalFragment().apply {
                retainInstance = true
                this.feedbackFlowListener = feedbackFlowListener
            }
    }
}
