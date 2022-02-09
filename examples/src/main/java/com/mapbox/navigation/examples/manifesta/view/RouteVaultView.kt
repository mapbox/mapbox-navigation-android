package com.mapbox.navigation.examples.manifesta.view

import android.app.AlertDialog
import android.app.Dialog
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.DialogFragment
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.navigation.examples.core.databinding.LayoutRouteVaultViewBinding
import com.mapbox.navigation.examples.manifesta.RouteVaultApi
import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteEntity
import com.mapbox.navigation.examples.manifesta.model.entity.StoredRouteRecord
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class RouteVaultView(
    private val routeVaultApi: RouteVaultApi,
    private val routeSelectedFun: (StoredRouteEntity) -> Unit
    ): DialogFragment() {

    private val viewBinding: LayoutRouteVaultViewBinding by lazy {
        LayoutRouteVaultViewBinding.inflate(layoutInflater)

    }

    private val routeVaultAdapter: RouteVaultAdapter by lazy {
        RouteVaultAdapter(::recordSelectedFun)
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val layoutManager = LinearLayoutManager(context)
        viewBinding.routeList.layoutManager = layoutManager
        viewBinding.routeList.adapter = routeVaultAdapter
        viewBinding.routeList.addItemDecoration(
            DividerItemDecoration(context, layoutManager.orientation)
        )

        viewBinding.btnClose.setOnClickListener {
            dismiss()
        }

        return AlertDialog.Builder(activity).also {
            it.setView(viewBinding.root)
        }.create()
    }

    override fun onResume() {
        super.onResume()
        CoroutineScope(Dispatchers.Main).launch {
            routeVaultApi.getRoutes().fold({
                Log.e(TAG, "Exception loading route records, ${it.message}")
            },{
                routeVaultAdapter.setData(it)
            })
        }
    }

    private fun recordSelectedFun(record: StoredRouteRecord) {
        CoroutineScope(Dispatchers.Main).launch {
            routeVaultApi.getRoute(record.id).value?.apply {
                routeSelectedFun(this)
                dismiss()
            }
        }
    }

    companion object {
        private const val TAG = "RouteVaultView"
    }
}
