package com.mapbox.navigation.qa_test_app.view

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.mapbox.navigation.qa_test_app.databinding.LayoutActivityIconsPreviewBinding

class IconsPreviewActivity : AppCompatActivity() {
    private val binding: LayoutActivityIconsPreviewBinding by lazy {
        LayoutActivityIconsPreviewBinding.inflate(layoutInflater)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(binding.root)
    }
}
