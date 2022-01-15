package com.mapbox.navigation.examples.manifesta.view

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.widget.Button
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.manifesta.model.entity.LocationCollectionEntity

class LocationsListDialog(private val itemClickFun: LocationCollectionSelectedConsumer): DialogFragment() {

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val rootView = layoutInflater.inflate(R.layout.location_chooser_layout, null).also { rootView ->
            rootView.findViewById<Button>(R.id.btnClose)?.setOnClickListener {
                dismiss()
            }

            rootView.findViewById<RecyclerView>(R.id.locationCollectionList)?.apply {
                this.layoutManager = LinearLayoutManager(rootView.context)
                this.adapter = LocationCollectionChooserAdapter(itemClickFun)
            }
        }

        return AlertDialog.Builder(activity).also {
            it.setView(rootView)
        }.create()
    }

    fun setLocationCollections(items: List<LocationCollectionEntity>) {
        (dialog?.findViewById<RecyclerView>(R.id.locationCollectionList)?.adapter as LocationCollectionChooserAdapter).setCollectionData(items)
    }
}
