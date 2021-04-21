package com.mapbox.navigation.qa_test_app.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.qa_test_app.R
import com.mapbox.navigation.qa_test_app.domain.TestActivityDescription

object ActivitiesListAdaptersSupport {
    val activitiesListOnBindViewHolderFun: (
        holder: ActivityViewHolder,
        position: Int,
        element: TestActivityDescription
    ) -> Unit = { holder, _, element ->
        holder.itemView.findViewById<TextView>(R.id.activityLabel)?.text = element.label
    }

    val viewHolderFactory: (parent: ViewGroup, viewType: Int) -> ActivityViewHolder = { parent, _ ->
        LayoutInflater.from(parent.context).inflate(
            R.layout.test_activity_item_layout, parent, false
        ).run {
            ActivityViewHolder(this)
        }
    }

    class ActivityViewHolder constructor(itemView: View) : RecyclerView.ViewHolder(itemView)
}
