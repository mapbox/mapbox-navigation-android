package com.mapbox.navigation.dropin.di

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.dropin.DropInNavigationView
import com.mapbox.navigation.dropin.DropInNavigationViewModel
import com.mapbox.navigation.dropin.databinding.DropInNavigationViewBinding
import dagger.BindsInstance
import dagger.Component
import javax.inject.Singleton

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Singleton
@Component(
    modules = [
        NavContextModule::class,
        CoroutinesModule::class
    ]
)
internal interface DropInUiInjector {

    fun inject(view: DropInNavigationView)

    @Component.Builder
    interface Builder {
        @BindsInstance
        fun context(context: Context): Builder

        @BindsInstance
        fun lifecycleOwner(owner: LifecycleOwner): Builder

        @BindsInstance
        fun viewModel(vm: DropInNavigationViewModel): Builder

        @BindsInstance
        fun rootViewBinding(binding: DropInNavigationViewBinding): Builder

        fun build(): DropInUiInjector
    }
}
