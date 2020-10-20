package com.mapbox.navigation.qa.view

import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.DividerItemDecoration
import androidx.recyclerview.widget.LinearLayoutManager
import com.mapbox.navigation.qa.R
import com.mapbox.navigation.qa.domain.CATEGORY_BUNDLE_KEY
import com.mapbox.navigation.qa.domain.TestActivitySuite.getCategoryActivityDescriptions
import com.mapbox.navigation.qa.domain.model.TestActivityDescription
import com.mapbox.navigation.qa.view.adapters.ActivitiesListAdapterSupport.activitiesListOnBindViewHolderFun
import com.mapbox.navigation.qa.view.adapters.ActivitiesListAdapterSupport.itemTypeProviderFun
import com.mapbox.navigation.qa.view.adapters.ActivitiesListAdapterSupport.viewHolderFactory
import com.mapbox.navigation.qa.view.adapters.GenericListAdapter
import com.mapbox.navigation.qa.view.adapters.GenericListAdapterItemSelectedFun
import com.mapbox.navigation.qa.view.adapters.GenericListAdapterSameItemFun
import kotlinx.android.synthetic.main.category_activities_layout.*

class CategoryActivitiesActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.category_activities_layout)

        val layoutManager = LinearLayoutManager(this)
        activitiesList.layoutManager = LinearLayoutManager(this)
        activitiesList.adapter = GenericListAdapter(
            activitiesListOnBindViewHolderFun,
            viewHolderFactory,
            activitySelectedDelegate,
            null,
            testActivityDescriptionSameItemFun,
            testActivityDescriptionSameItemFun,
            itemTypeProviderFun,
            mapOf(Pair(R.id.infoLabel, infoIconClickListenerFun))
        )
        activitiesList.addItemDecoration(DividerItemDecoration(this, layoutManager.orientation))

    }

    override fun onStart() {
        super.onStart()
        intent.extras!!.getString(CATEGORY_BUNDLE_KEY)?.let { categoryName ->
            getCategoryActivityDescriptions(categoryName).let { descriptions ->
                (activitiesList.adapter as GenericListAdapter<TestActivityDescription, *>).let { adapter ->
                    adapter.swap(descriptions)
                }
            }
        }
    }

    private val activitySelectedDelegate: GenericListAdapterItemSelectedFun<TestActivityDescription> = { positionAndElement ->
        positionAndElement.second.launchActivityFun(this)
    }

    private val testActivityDescriptionSameItemFun: GenericListAdapterSameItemFun<TestActivityDescription> = { item1, item2 ->
        item1 == item2
    }

    private val infoIconClickListenerFun: GenericListAdapterItemSelectedFun<TestActivityDescription> = { positionAndElement ->
        AlertDialog.Builder(this)
            .setMessage(positionAndElement.second.fullDescriptionResource)
            .setTitle("Test Description")
            .setPositiveButton("Ok") { dlg, _ ->
                dlg.dismiss()
            }.show()
    }
}
