package com.mapbox.navigation.examples.manifesta.view

import android.app.AlertDialog
import android.app.Dialog
import android.content.Context
import android.graphics.Point
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.mapbox.navigation.examples.core.R
import com.mapbox.navigation.examples.core.databinding.LayoutRouteVaultViewBinding
import com.mapbox.navigation.examples.manifesta.RouteVaultApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RouteVaultView(private val routeVaultApi: RouteVaultApi): DialogFragment() {

    // private val viewBinding: LayoutRouteVaultViewBinding by lazy {
    //     LayoutRouteVaultViewBinding.inflate(layoutInflater)
    //
    // }

    private val adapter: RouteVaultAdapter by lazy {
        RouteVaultAdapter()
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val routeView = layoutInflater.inflate(R.layout.layout_route_vault_view, null)

        routeView.findViewById<RecyclerView>(R.id.routeList).apply {
            val layoutManager = LinearLayoutManager(context)
            this.layoutManager = layoutManager
            this.adapter = adapter
            this.addItemDecoration(
                DividerItemDecoration(context, layoutManager.orientation)
            )
        }


        return AlertDialog.Builder(activity).also {
            it.setView(routeView)
        }.create()
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.Main).launch {
            routeVaultApi.getRoutes().fold({
                Log.e(TAG, "Exception loading route records, ${it.message}")
            },{
                adapter.setData(it)
            })
        }
    }

    companion object {
        private const val TAG = "RouteVaultView"
    }
}
