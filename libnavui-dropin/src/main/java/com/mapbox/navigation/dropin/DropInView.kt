package com.mapbox.navigation.dropin

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.mapbox.maps.MapInitOptions
import com.mapbox.maps.MapView
import com.mapbox.maps.ResourceOptions
import com.mapbox.navigation.dropin.databinding.MapboxLayoutDropInViewBinding

class DropInView: ConstraintLayout, LifecycleObserver {

    private lateinit var  dropInViewOptions: DropInViewOptions
    private lateinit var lifeCycleOwner: LifecycleOwner

    private val binding = MapboxLayoutDropInViewBinding.inflate(
        LayoutInflater.from(context),
        this
    )

    constructor(context: Context) : this(context, null)

    constructor(context: Context, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int
    ) : super(context, attrs, defStyleAttr)

    constructor(
        context: Context,
        attrs: AttributeSet?,
        defStyleAttr: Int,
        lifecycleOwner: LifecycleOwner,
        dropInViewOptions: DropInViewOptions,

    ) : super(context, attrs, defStyleAttr) {
        this.lifeCycleOwner = lifecycleOwner
        this.dropInViewOptions = dropInViewOptions
    }

    init {
        lifeCycleOwner.lifecycle.addObserver(this)
        val mapView = MapView(
            context,
            MapInitOptions(
                context,
                ResourceOptions.Builder()
                    .accessToken("YOUR_ACCESS_TOKEN")
                    .build()
            )
        )
        binding.mapContainer.addView(mapView)
    }
}

