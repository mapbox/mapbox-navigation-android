package com.mapbox.navigation.qa.view.adapters

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.qa.R
import com.mapbox.navigation.qa.domain.model.TestActivityCategory
import kotlinx.android.synthetic.main.category_layout.view.*

object CategoryListAdapterSupport {

    val categoryListOnBindViewHolderFun: (
        holder: CategoryViewHolder,
        position: Int,
        element: TestActivityCategory
    ) -> Unit = { holder, _, element ->
        holder.itemView.categoryName.text = element.label
    }

    val viewHolderFactory: (parent: ViewGroup, viewType: Int) -> CategoryViewHolder = { parent, viewType ->
        LayoutInflater.from(parent.context).inflate(viewType, parent, false).run {
            CategoryViewHolder(this)
        }
    }

    val itemTypeProviderFun: GenericListAdapterGetItemViewTypeFun = { _ -> R.layout.category_layout }
}
class CategoryViewHolder constructor(itemView: View): RecyclerView.ViewHolder(itemView)
