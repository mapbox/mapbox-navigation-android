package com.mapbox.navigation.examples

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.synthetic.main.item_examples_adapter.view.*

class ExamplesAdapter(
    appContext: Context,
    private val itemClickLambda: (position: Int) -> Unit
) : RecyclerView.Adapter<ExamplesAdapter.ExamplesViewHolder>() {

    private val inflater = LayoutInflater.from(appContext)
    private val itemList: MutableList<SampleItem> = mutableListOf()

    fun addSampleItems(mutableList: List<SampleItem>) {
        itemList.addAll(mutableList)
        notifyItemRangeInserted(0, mutableList.size)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ExamplesViewHolder {
        val view = inflater.inflate(R.layout.item_examples_adapter, parent, false)
        return ExamplesViewHolder(view)
    }

    override fun getItemCount() = itemList.size

    override fun onBindViewHolder(holder: ExamplesViewHolder, position: Int) {
        holder.bindItem(itemList[position])
    }

    inner class ExamplesViewHolder(private val view: View) : RecyclerView.ViewHolder(view) {
        fun bindItem(sampleItem: SampleItem) {
            view.nameView.text = sampleItem.name
            view.descriptionView.text = sampleItem.description

            view.setOnClickListener {
                itemClickLambda(layoutPosition)
            }
        }
    }
}
