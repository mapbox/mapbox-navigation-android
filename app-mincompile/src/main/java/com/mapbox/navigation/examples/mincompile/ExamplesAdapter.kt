package com.mapbox.navigation.examples.mincompile

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.examples.mincompile.databinding.ItemExamplesAdapterBinding

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
        val binding = ItemExamplesAdapterBinding.inflate(inflater, parent, false)
        return ExamplesViewHolder(binding)
    }

    override fun getItemCount() = itemList.size

    override fun onBindViewHolder(holder: ExamplesViewHolder, position: Int) {
        holder.bindItem(itemList[position])
    }

    inner class ExamplesViewHolder(
        private val viewBinding: ItemExamplesAdapterBinding
    ) : RecyclerView.ViewHolder(viewBinding.root) {
        fun bindItem(sampleItem: SampleItem) {
            viewBinding.nameView.text = sampleItem.name
            viewBinding.descriptionView.text = sampleItem.description

            viewBinding.root.setOnClickListener {
                itemClickLambda(layoutPosition)
            }
        }
    }
}
