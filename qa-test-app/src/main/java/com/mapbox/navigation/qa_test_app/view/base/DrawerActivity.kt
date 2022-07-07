package com.mapbox.navigation.qa_test_app.view.base

import android.os.Bundle
import android.view.View
import androidx.annotation.CallSuper
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.GravityCompat
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityDrawerActivityBinding

abstract class DrawerActivity : AppCompatActivity() {

    private lateinit var binding: LayoutActivityDrawerActivityBinding

    @CallSuper
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = LayoutActivityDrawerActivityBinding.inflate(layoutInflater)
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
}
