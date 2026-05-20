package dev.karipap.app.di

import android.app.Activity
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dev.karipap.app.R
import dev.karipap.app.input.InputTesterController
import dev.karipap.app.ui.viewmodel.InputTesterViewModel
import dev.cannoli.ui.components.OsdController

@Module
@InstallIn(ActivityComponent::class)
object InputModule {

    @Provides @ActivityScoped
    fun provideOsdController(): OsdController = OsdController()

    @Provides @ActivityScoped
    fun provideInputTesterController(
        activity: Activity,
        viewModel: InputTesterViewModel,
        portRouter: dev.karipap.app.input.runtime.PortRouter,
    ): InputTesterController = InputTesterController(
        viewModel = viewModel,
        portRouter = portRouter,
        unknownDeviceName = activity.getString(R.string.input_tester_device_unknown),
        keyboardDeviceName = activity.getString(R.string.input_tester_device_keyboard),
    )
}
