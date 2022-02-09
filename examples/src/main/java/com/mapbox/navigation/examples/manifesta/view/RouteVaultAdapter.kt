package com.mapbox.navigation.examples.manifesta.view

import android.util.Log
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.examples.core.databinding.LayoutRouteVaultRecordBinding
import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteRecord

class RouteVaultAdapter(private val itemSelectedFun: (StoredRouteRecord) -> Unit): RecyclerView.Adapter<RouteVaultViewHolder>() {
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
        holder.bind(routeRecords[position], ::deleteItemClicked)
    }

    fun setData(items: List<StoredRouteRecord>) {
        routeRecords.clear()
        routeRecords.addAll(items)
        notifyDataSetChanged()
    }

    private fun deleteItemClicked(record: StoredRouteRecord) {
        Log.e("foobar", "delete clicked")
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
    }
}
