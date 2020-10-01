package com.mapbox.navigation.ui.feedback

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.InputMethodManager
import androidx.core.content.ContextCompat
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.Fragment
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetBehavior.BottomSheetCallback
import com.google.android.material.bottomsheet.BottomSheetBehavior.from
import com.google.android.material.snackbar.Snackbar
import com.mapbox.navigation.core.telemetry.events.FeedbackEvent
import com.mapbox.navigation.ui.R
import kotlinx.android.synthetic.main.mapbox_edit_text_feedback_optional_comment.feedbackCommentEditText
import kotlinx.android.synthetic.main.mapbox_feedback_arrival_bottom_sheet.feedbackArrivalBottomSheet
import kotlinx.android.synthetic.main.mapbox_feedback_arrival_fragment.mapbox_feedback_arrival_fragment_parent_view_group
import kotlinx.android.synthetic.main.mapbox_partial_feedback_arrival_button_bar.feedbackArrivalFinishButton
import kotlinx.android.synthetic.main.mapbox_partial_feedback_bottom_sheet_top_banner.cancelBtn
import kotlinx.android.synthetic.main.mapbox_partial_feedback_bottom_sheet_top_banner.feedbackBottomSheetTitleText
import timber.log.Timber

/**
 * This [Fragment] is responsible for showing UI related to providing
 * information.
 */
class FeedbackArrivalFragment : DialogFragment() {

    private lateinit var bottomSheetBehavior: BottomSheetBehavior<View>
    private var feedbackFlowListener: FeedbackFlowListener? = null
    private val feedbackList = ArrayList<FeedbackItem>()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.mapbox_feedback_arrival_fragment, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        cancelBtn.setColorFilter(Color.WHITE)
        initBottomSheetBehavior()
        initTitleTextView()
        initListeners()
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
            if (feedbackCommentEditText.text.isNotEmpty()) {
                saveComment()
                hideSoftInput(feedbackCommentEditText)
                dismiss()
            }
            Snackbar.make(mapbox_feedback_arrival_fragment_parent_view_group,
                if (feedbackCommentEditText.text.isEmpty()) R.string.mapbox_feedback_arrival_experience_empty else R.string.mapbox_feedback_reported,
                Snackbar.LENGTH_SHORT).apply {
                    view.setBackgroundColor(ContextCompat.getColor(context, R.color.mapbox_feedback_bottom_sheet_tertiary))
                    setTextColor(ContextCompat.getColor(context, R.color.mapbox_feedback_bottom_sheet_primary_text))
                    show()
            }
        }
        feedbackCommentEditText.setOnTouchListener { v, event ->
            v.parent.requestDisallowInterceptTouchEvent(true)
            if (event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_UP) {
                v.parent.requestDisallowInterceptTouchEvent(false)
            }
            false
        }
    }

    private fun initBottomSheetBehavior() {
        bottomSheetBehavior = from(feedbackArrivalBottomSheet)
        bottomSheetBehavior.addBottomSheetCallback(bottomSheetCallBack)
    }

    private fun saveComment() {
       val arrivalExperienceFeedbackItem = FeedbackItem(getString(R.string.mapbox_feedback_type_general),
            PLACEHOLDER_INT_FOR_GENERAL_FEEDBACK_DRAWABLE,
            FeedbackEvent.ARRIVAL_FEEDBACK,
            feedbackCommentEditText.text.toString())
        feedbackList?.add(arrivalExperienceFeedbackItem)
        feedbackFlowListener?.onArrivalExperienceFeedbackFinished(arrivalExperienceFeedbackItem)
    }

    private fun hideSoftInput(view: View) {
        val imm = view.context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
        view.clearFocus()
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
        private const val PLACEHOLDER_INT_FOR_GENERAL_FEEDBACK_DRAWABLE = 0

        fun newInstance(feedbackFlowListener: FeedbackFlowListener?, feedbackItemList: List<FeedbackItem>?): FeedbackArrivalFragment =
            FeedbackArrivalFragment().apply {
                feedbackFlowListener?.let {
                    this.feedbackFlowListener = feedbackFlowListener
                }
                feedbackItemList?.let {
                    feedbackList.addAll(feedbackItemList)
                }
            }
    }
}
