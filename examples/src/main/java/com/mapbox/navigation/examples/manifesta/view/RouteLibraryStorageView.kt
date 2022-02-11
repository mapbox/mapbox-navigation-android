package com.mapbox.navigation.examples.manifesta.view

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.fragment.app.DialogFragment
import com.mapbox.api.directions.v5.models.DirectionsRoute
import com.mapbox.navigation.examples.core.databinding.LayoutRouteLibraryStorageBinding
import com.mapbox.navigation.examples.manifesta.RouteVaultApi
import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RouteLibraryStorageView(
    private val routeVaultApi: RouteVaultApi,
    private val routeToStore: DirectionsRoute
    ): DialogFragment() {

    private val viewBinding: LayoutRouteLibraryStorageBinding by lazy {
        LayoutRouteLibraryStorageBinding.inflate(layoutInflater)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        viewBinding.btnSave.setOnClickListener(::saveButtonHandler)
        viewBinding.btnClose.setOnClickListener { dismiss() }

        return AlertDialog.Builder(activity).also {
            it.setView(viewBinding.root)
        }.create()
    }

    private fun saveButtonHandler(view: View) {
        when (viewBinding.routeName.text?.isEmpty()) {
            true -> {
                viewBinding.routeNameContainer.isErrorEnabled
                viewBinding.routeNameContainer.error = "Please give this route a name."
                return
            }
            false -> {
                viewBinding.routeNameContainer.isErrorEnabled = false
            }
        }

        val entity = StoredRouteEntity(
            alias = viewBinding.routeName.text.toString().trim(),
            routeAsJson = routeToStore.toJson()
        )
        CoroutineScope(Dispatchers.Main).launch {
            routeVaultApi.storeRoute(entity).onError {
                Log.e(TAG, "Failed to store route in library.", it)
            }
        }
        dismiss()
    }

    companion object {
        private const val TAG = "RouteLibraryStorageView"
    }
}
