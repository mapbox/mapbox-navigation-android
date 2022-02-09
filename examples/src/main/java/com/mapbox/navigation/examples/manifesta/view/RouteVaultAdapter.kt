package com.mapbox.navigation.examples.manifesta.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.databinding.LayoutRouteVaultRecordBinding
import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteRecord

class RouteVaultAdapter: RecyclerView.Adapter<RouteVaultViewHolder>() {
    private val routeRecords = mutableListOf<StoredRouteRecord>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RouteVaultViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.layout_route_vault_record, parent, false)

        //val viewBinding = LayoutRouteVaultRecordBinding.inflate(LayoutInflater.from(parent.context))
        return RouteVaultViewHolder(v)
    }

    override fun getItemCount(): Int = routeRecords.size

    override fun onBindViewHolder(holder: RouteVaultViewHolder, position: Int) {
        holder.bind(routeRecords[position])
    }

    fun setData(items: List<StoredRouteRecord>) {
        routeRecords.clear()
        routeRecords.addAll(items)
        notifyDataSetChanged()
    }

}
class RouteVaultViewHolder constructor(itemView: View): RecyclerView.ViewHolder(itemView) {
    fun bind(record: StoredRouteRecord) {
        itemView.findViewById<TextView>(R.id.routeAliasLabel).text = record.alias
        //viewBinding.routeAliasLabel.text = record.alias
    }
}
