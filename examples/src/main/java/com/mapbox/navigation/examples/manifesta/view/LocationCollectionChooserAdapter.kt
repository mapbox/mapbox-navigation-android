package com.mapbox.navigation.examples.manifesta.view

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.manifesta.model.entity.LocationCollectionEntity

/**
 * Created on 2/5/18.
 */
class LocationCollectionChooserAdapter(val itemClickFun: LocationCollectionSelectedConsumer): RecyclerView.Adapter<LocationCollectionViewHolder>() {

    private val locationCollections = mutableListOf<LocationCollectionEntity>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LocationCollectionViewHolder {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.location_collection_item_layout, parent, false)
        return LocationCollectionViewHolder(v)
    }

    override fun getItemCount(): Int = locationCollections.size

    override fun onBindViewHolder(holder: LocationCollectionViewHolder, position: Int) {
        holder.itemView.findViewById<TextView>(R.id.locationCollectionName)?.apply {
            this.text = locationCollections[position].name
            setOnClickListener {
                itemClickFun(locationCollections[position])
            }
        }
    }

    fun setCollectionData(items: List<LocationCollectionEntity>) {
        locationCollections.clear()
        locationCollections.addAll(items)
        notifyDataSetChanged()
    }
}
class LocationCollectionViewHolder constructor(itemView: View): RecyclerView.ViewHolder(itemView)
typealias LocationCollectionSelectedConsumer = (LocationCollectionEntity) -> Unit
