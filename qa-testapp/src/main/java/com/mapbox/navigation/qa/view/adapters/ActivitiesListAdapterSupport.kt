package com.mapbox.navigation.qa.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.qa.R
import com.mapbox.navigation.qa.domain.model.TestActivityDescription
import kotlinx.android.synthetic.main.test_activity_item_layout.view.*

object ActivitiesListAdapterSupport {

    val activitiesListOnBindViewHolderFun: (
        holder: ActivityViewHolder,
        position: Int,
        element: TestActivityDescription
    ) -> Unit = { holder, _, element ->
        holder.itemView.activityLabel.text = element.label
    }

    val viewHolderFactory: (parent: ViewGroup, viewType: Int) -> ActivityViewHolder = { parent, viewType ->
        LayoutInflater.from(parent.context).inflate(viewType, parent, false).run {
            ActivityViewHolder(this)
        }
    }

    val itemTypeProviderFun: GenericListAdapterGetItemViewTypeFun = { _ -> R.layout.test_activity_item_layout }
}
class ActivityViewHolder constructor(itemView: View): RecyclerView.ViewHolder(itemView)
