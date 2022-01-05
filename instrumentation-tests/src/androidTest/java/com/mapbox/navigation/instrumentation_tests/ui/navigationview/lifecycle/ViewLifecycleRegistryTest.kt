package com.mapbox.navigation.instrumentation_tests.ui.navigationview.lifecycle

import android.content.Context
import android.location.Location
import android.view.View
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewModelScope
import androidx.test.espresso.Espresso
import androidx.test.espresso.IdlingResource
import com.mapbox.navigation.instrumentation_tests.activity.EmptyTestActivity
import com.mapbox.navigation.instrumentation_tests.utils.runOnMainSync
import com.mapbox.navigation.testing.ui.BaseTest
import com.mapbox.navigation.testing.ui.idling.NavigationIdlingResource
import com.mapbox.navigation.ui.utils.internal.lifecycle.ViewLifecycleRegistry
import com.mapbox.navigation.ui.utils.internal.lifecycle.keepExecutingWhenStarted
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import org.junit.Assert
import org.junit.Test

class ViewLifecycleRegistryTest : BaseTest<EmptyTestActivity>(EmptyTestActivity::class.java) {
    override fun setupMockLocation(): Location {
        return mockLocationUpdatesRule.generateLocationUpdate {
            latitude = 0.0
            longitude = 0.0
        }
    }

    @Test
    fun view_model_keeps_counting_while_view_is_detached() {
        var viewRef: DummyView? = null
        runOnMainSync {
            // view needs to be crated on the main thread to initialize the view model correctly
            viewRef = DummyView(activity)
        }
        val view: DummyView = viewRef!!

        // add view and count to five, view should capture all values from the view model
        val viewIdlingResource5 = EqualityIdlingResource("View5", 5)
        val viewModelIdlingResource5 = EqualityIdlingResource("ViewModel5", 5)
        viewIdlingResource5.register()
        viewModelIdlingResource5.register()
        runOnMainSync {
            activity.binding.root.addView(view)
            view.localIdlingResource = viewIdlingResource5
            view.viewModel.viewModelIdlingResource = viewModelIdlingResource5
        }
        Espresso.onIdle()
        viewIdlingResource5.unregister()
        viewModelIdlingResource5.unregister()

        // remove the view which will cancel its flow collector while view model keeps counting
        // because its bound to the activity lifecycle which is still alive
        val viewModelIdlingResource10 = EqualityIdlingResource("ViewModel10", 10)
        viewModelIdlingResource10.register()
        runOnMainSync {
            activity.binding.root.removeView(view)
            view.viewModel.viewModelIdlingResource = viewModelIdlingResource10
        }
        Espresso.onIdle()
        viewModelIdlingResource10.unregister()
        Assert.assertEquals(5, view.localCounter) // view ignored view model updates
        Assert.assertEquals(10, view.viewModel.viewModelCounter)

        // re-add the view to the window which re-subscribes the flow collector and receives latest
        val viewIdlingResource10 = EqualityIdlingResource("View10", 10)
        viewIdlingResource10.register()
        runOnMainSync {
            activity.binding.root.addView(view)
            view.localIdlingResource = viewIdlingResource10
        }
        Espresso.onIdle()
        viewIdlingResource10.unregister()
        Assert.assertEquals(10, view.localCounter)
    }
}

private class DummyView(context: Context) : View(context), LifecycleOwner {
    private val viewLifecycleRegistry = ViewLifecycleRegistry(
        hostingLifecycleOwner = context as LifecycleOwner,
        localLifecycleOwner = this,
        view = this
    )
    val viewModel = ViewModelProvider(context as ViewModelStoreOwner)[DummyViewModel::class.java]
    var localCounter = 0
    var localIdlingResource: EqualityIdlingResource<Int>? = null
        set(value) {
            field = value
            value?.currentValue = localCounter
        }

    init {
        keepExecutingWhenStarted {
            viewModel.counterFlow.collect {
                localCounter = it
                localIdlingResource?.currentValue = localCounter
            }
        }
    }

    override fun getLifecycle(): Lifecycle = viewLifecycleRegistry
}

class DummyViewModel : ViewModel() {
    private val _counterFlow = MutableStateFlow(0)
    val counterFlow = _counterFlow.asStateFlow()
    var viewModelCounter = 0
    var viewModelIdlingResource: EqualityIdlingResource<Int>? = null
        set(value) {
            field = value
            value?.currentValue = viewModelCounter
        }

    init {
        viewModelScope.launch {
            while (isActive) {
                viewModelCounter++
                _counterFlow.value = viewModelCounter
                viewModelIdlingResource?.currentValue = viewModelCounter
                delay(100)
            }
        }
    }
}

class EqualityIdlingResource<T>(
    private val name: String,
    private val targetValue: T
) : NavigationIdlingResource() {
    var currentValue: T? = null
        set(value) {
            field = value
            if (isIdleNow) {
                callback?.onTransitionToIdle()
            }
        }
    private var callback: IdlingResource.ResourceCallback? = null

    override fun getName() = name

    override fun isIdleNow() = currentValue == targetValue

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        this.callback = callback
        if (isIdleNow) {
            callback?.onTransitionToIdle()
        }
    }
}
