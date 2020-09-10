package com.mapbox.navigation.ui.feedback

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.ui.R

/**
 * FeedbackSubTypeAdapter provides a binding from [FeedbackBottomSheet] data set
 * to [FeedbackSubTypeViewHolder] that are displayed within a [RecyclerView].
 */
internal class FeedbackSubTypeAdapter constructor(
    private val itemClickListener: OnSubTypeItemClickListener
) : ListAdapter<FeedbackSubTypeItem, FeedbackSubTypeViewHolder>(DiffCallback()) {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): FeedbackSubTypeViewHolder {
        return FeedbackSubTypeViewHolder(
            LayoutInflater.from(parent.context)
                .inflate(R.layout.mapbox_item_feedback_detail, parent, false),
            itemClickListener
        )
    }

    override fun onBindViewHolder(
        holder: FeedbackSubTypeViewHolder,
        position: Int
    ) {
        holder.setFeedbackSubTypeInfo(getItem(position))
    }

    fun getFeedbackSubTypeItem(position: Int): FeedbackSubTypeItem {
        return getItem(position)
    }

    internal interface OnSubTypeItemClickListener {
        fun onItemClick(position: Int): Boolean
    }

    class DiffCallback : DiffUtil.ItemCallback<FeedbackSubTypeItem>() {
        override fun areItemsTheSame(
            oldItem: FeedbackSubTypeItem,
            newItem: FeedbackSubTypeItem
        ): Boolean {
            return oldItem.feedbackDescription == newItem.feedbackDescription &&
                oldItem.isChecked == newItem.isChecked
        }

        override fun areContentsTheSame(
            oldItem: FeedbackSubTypeItem,
            newItem: FeedbackSubTypeItem
        ): Boolean {
            return oldItem.feedbackDescription == newItem.feedbackDescription &&
                oldItem.isChecked == newItem.isChecked
        }
    }
}
