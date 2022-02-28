package com.mapbox.navigation.dropin.di

import android.content.Context
import androidx.lifecycle.LifecycleOwner
import com.mapbox.api.directions.v5.models.RouteOptions
import com.mapbox.navigation.base.ExperimentalPreviewMapboxNavigationAPI
import com.mapbox.navigation.base.extensions.applyDefaultNavigationOptions
import com.mapbox.navigation.base.extensions.applyLanguageAndVoiceUnitOptions
import com.mapbox.navigation.core.MapboxNavigation
import com.mapbox.navigation.core.lifecycle.MapboxNavigationApp
import com.mapbox.navigation.dropin.DropInNavigationViewContext
import com.mapbox.navigation.dropin.DropInNavigationViewModel
import com.mapbox.navigation.dropin.component.location.LocationBehavior
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Named
import javax.inject.Singleton

@OptIn(ExperimentalPreviewMapboxNavigationAPI::class)
@Module
internal class NavContextModule {

    @Provides
    fun navigation(): MapboxNavigation {
        return checkNotNull(MapboxNavigationApp.current()) {
            "MapboxNavigationApp not initialized. You must call MapboxNavigationApp.setup() first."
        }
    }

    @Singleton
    @Provides
    fun dropInNavigationViewContext(
        context: Context,
        lifecycleOwner: LifecycleOwner,
        viewModel: DropInNavigationViewModel
    ) = DropInNavigationViewContext(context, lifecycleOwner, viewModel)

    @Provides
    fun locationBehavior(
        viewModel: DropInNavigationViewModel
    ): LocationBehavior = viewModel.locationBehavior

    @Named("fetchRoute")
    @Provides
    fun routeOptionsBuilder(
        context: Context
    ): RouteOptions.Builder {
        return RouteOptions.builder()
            .applyDefaultNavigationOptions()
            .applyLanguageAndVoiceUnitOptions(context)
    }

    @Named("activeNavigationStarted")
    @Provides
    fun activeNavigationStarted(
        viewModel: DropInNavigationViewModel
    ): MutableStateFlow<Boolean> = viewModel._activeNavigationStarted
}
