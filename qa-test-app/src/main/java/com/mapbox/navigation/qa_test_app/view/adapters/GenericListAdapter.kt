package com.mapbox.navigation.qa_test_app.view.adapters

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView

typealias GenericListAdapterSameItemFun<T> = (T, T) -> Boolean
typealias GenericListAdapterGetItemViewTypeFun = (Int) -> Int
typealias GenericListAdapterItemSelectedFun<T> = (Int, T) -> Unit

class GenericListAdapter<T, U : RecyclerView.ViewHolder>(

    val onBindViewHolder: (holder: U, position: Int, element: T) -> Unit,
    val viewHolderFactory: (parent: ViewGroup, viewType: Int) -> U,
    val itemSelectedDelegate: GenericListAdapterItemSelectedFun<T>? = null,
    val longPressDelegate: GenericListAdapterItemSelectedFun<T>? = null,
    val sameIdFun: GenericListAdapterSameItemFun<T>? = { item1, item2 ->
        item1 == item2
    },
    val sameContentFun: GenericListAdapterSameItemFun<T>? = { item1, item2 ->
        item1 == item2
    },
    val itemTypeFun: GenericListAdapterGetItemViewTypeFun? = null,
    val auxViewClickMap: Map<Int, GenericListAdapterItemSelectedFun<T>>? = null

) : RecyclerView.Adapter<U>() {

    private val values: MutableList<T> = mutableListOf()

    override fun getItemCount(): Int = values.size

    override fun onBindViewHolder(holder: U, position: Int) {

        val element: T = values[position]
        itemSelectedDelegate?.let {
            holder.itemView.setOnClickListener {
                itemSelectedDelegate.invoke(position, element)
            }
        }

        longPressDelegate?.let {
            holder.itemView.setOnLongClickListener {
                longPressDelegate.invoke(position, element)
                true
            }
        }

        auxViewClickMap?.forEach { entry ->
            holder.itemView.findViewById<View>(entry.key)
                ?.setOnClickListener { entry.value.invoke(position, element) }
        }

        onBindViewHolder.invoke(holder, position, element)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): U =
        viewHolderFactory(parent, viewType)

    override fun getItemViewType(position: Int): Int =
        itemTypeFun?.invoke(position) ?: super.getItemViewType(position)

    fun getCloneOfValues(): List<T> = values.toList()

    fun swap(newValues: List<T>) {
        val oldValues = values.toList()
        val result = DiffUtil.calculateDiff(createElementsDiffCallback(oldValues, newValues))

        values.clear()
        values.addAll(newValues)

        result.dispatchUpdatesTo(this)
    }

    private fun createElementsDiffCallback(
        oldValues: List<T>,
        newValues: List<T>
    ): DiffUtil.Callback {

        return object : DiffUtil.Callback() {
            override fun getOldListSize(): Int = oldValues.size
            override fun getNewListSize(): Int = newValues.size
            override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                sameIdFun?.invoke(oldValues[oldItemPosition], newValues[newItemPosition]) ?: false
            override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean =
                sameContentFun?.invoke(oldValues[oldItemPosition], newValues[newItemPosition])
                    ?: false
        }
    }

    fun getItemAtPosition(position: Int): T? = values.firstOrNull { it == values[position] }
}
