package com.mapbox.navigation.examples.manifesta.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.examples.core.databinding.LayoutRouteVaultRecordBinding
import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteRecord

class RouteVaultAdapter(
    private val itemSelectedFun: (StoredRouteRecord) -> Unit,
    private val deleteItemFun: (StoredRouteRecord) -> Unit
): RecyclerView.Adapter<RouteVaultViewHolder>() {

    private val routeRecords = mutableListOf<StoredRouteRecord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteVaultViewHolder {
        val viewBinding = LayoutRouteVaultRecordBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return RouteVaultViewHolder(viewBinding)
    }

    override fun getItemCount(): Int = routeRecords.size

    override fun onBindViewHolder(holder: RouteVaultViewHolder, position: Int) {
        holder.itemView.setOnClickListener {
            itemSelectedFun(routeRecords[position])
        }
        holder.itemView.setOnLongClickListener {
            holder.toggleDeleteButtonVisibility()
            true
        }
        holder.bind(routeRecords[position], ::deleteItemClicked)
    }

    fun setData(items: List<StoredRouteRecord>) {
        routeRecords.clear()
        routeRecords.addAll(items)
        notifyDataSetChanged()
    }

    fun cloneData(): List<StoredRouteRecord> = routeRecords.toList()

    private fun deleteItemClicked(record: StoredRouteRecord) {
        deleteItemFun(record)
    }

}
class RouteVaultViewHolder constructor(
    private val viewBinding: LayoutRouteVaultRecordBinding
): RecyclerView.ViewHolder(viewBinding.root) {
    fun bind(record: StoredRouteRecord, itemDeleteHandler: (StoredRouteRecord) -> Unit) {
        viewBinding.routeAliasLabel.text = record.alias
        viewBinding.btnDeleteRecord.setOnClickListener {
            itemDeleteHandler(record)
        }
        viewBinding.btnDeleteRecord.visibility = View.INVISIBLE
    }

    fun toggleDeleteButtonVisibility() {
        viewBinding.btnDeleteRecord.visibility = when (viewBinding.btnDeleteRecord.visibility) {
            View.VISIBLE -> View.INVISIBLE
            View.INVISIBLE -> View.VISIBLE
            else -> View.INVISIBLE
        }
    }
}
