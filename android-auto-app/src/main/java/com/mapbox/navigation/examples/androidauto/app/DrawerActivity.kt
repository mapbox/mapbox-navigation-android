package com.mapbox.navigation.examples.androidauto.app

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.SpinnerAdapter
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.AppCompatSpinner
import androidx.appcompat.widget.SwitchCompat
import androidx.core.view.GravityCompat
import androidx.lifecycle.MutableLiveData
import com.mapbox.navigation.examples.androidauto.databinding.ActivityDrawerBinding

abstract class DrawerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDrawerBinding

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDrawerBinding.inflate(layoutInflater)
        binding.drawerContent.addView(onCreateContentView(), 0)
        binding.drawerMenuContent.addView(onCreateMenuView())
        setContentView(binding.root)

        binding.menuButton.setOnClickListener { openDrawer() }
    }

    abstract fun onCreateContentView(): View

    abstract fun onCreateMenuView(): View

    fun openDrawer() {
        binding.drawerLayout.openDrawer(GravityCompat.START)
    }

    fun closeDrawers() {
        binding.drawerLayout.closeDrawers()
    }

    protected fun bindSwitch(
        switch: SwitchCompat,
        getValue: () -> Boolean,
        setValue: (v: Boolean) -> Unit
    ) {
        switch.isChecked = getValue()
        switch.setOnCheckedChangeListener { _, isChecked -> setValue(isChecked) }
    }

    protected fun bindSwitch(
        switch: SwitchCompat,
        liveData: MutableLiveData<Boolean>,
        onChange: (value: Boolean) -> Unit
    ) {
        liveData.observe(this) {
            switch.isChecked = it
            onChange(it)
        }
        switch.setOnCheckedChangeListener { _, isChecked ->
            liveData.value = isChecked
        }
    }

    protected fun bindSpinner(
        spinner: AppCompatSpinner,
        liveData: MutableLiveData<String>,
        onChange: (value: String) -> Unit
    ) {
        liveData.observe(this) {
            if (spinner.selectedItem != it) {
                spinner.setSelection(spinner.adapter.findItemPosition(it) ?: 0)
            }
            onChange(it)
        }

        spinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    liveData.value = parent.getItemAtPosition(position) as? String
                }

                override fun onNothingSelected(parent: AdapterView<*>?) = Unit
            }
    }

    private fun SpinnerAdapter.findItemPosition(item: Any): Int? {
        for (pos in 0..count) {
            if (item == getItem(pos)) return pos
        }
        return null
    }
}
