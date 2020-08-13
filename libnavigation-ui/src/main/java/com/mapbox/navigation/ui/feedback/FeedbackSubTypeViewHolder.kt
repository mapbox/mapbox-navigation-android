package com.mapbox.navigation.ui.feedback

import android.view.View
import android.widget.CheckBox
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.ui.R
import com.mapbox.navigation.ui.feedback.FeedbackSubTypeAdapter.OnSubTypeItemClickListener

/**
 * A ViewHolder describes a Feedback subtype view and metadata about its place within the RecyclerView.
 */
internal class FeedbackSubTypeViewHolder(
    itemView: View,
    private val itemClickListener: OnSubTypeItemClickListener?
) : RecyclerView.ViewHolder(itemView),
    View.OnClickListener {

    private val checkBox: CheckBox = itemView.findViewById(R.id.checkbox)
    private val feedbackSubType: TextView = itemView.findViewById(R.id.feedbackSubType)

    init {
        itemView.setOnClickListener(this)
        checkBox.setOnClickListener(this)
    }

    fun setFeedbackSubTypeInfo(item: FeedbackSubTypeItem) {
        checkBox.isChecked = item.isChecked
        feedbackSubType.setText(item.feedbackDescriptionResourceId)
    }

    override fun onClick(view: View) {
        if (itemClickListener != null) {
            checkBox.isChecked = itemClickListener.onItemClick(adapterPosition)
        }
    }
}
