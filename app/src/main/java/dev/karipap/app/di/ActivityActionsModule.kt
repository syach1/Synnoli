package dev.karipap.app.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.qualifiers.ActivityContext
import dagger.hilt.android.scopes.ActivityScoped
import dev.karipap.app.MainActivity
import dev.karipap.app.input.ActivityActions

@Module
@InstallIn(ActivityComponent::class)
object ActivityActionsModule {
    @Provides @ActivityScoped
    fun provideActivityActions(@ActivityContext activity: android.content.Context): ActivityActions =
        activity as MainActivity
}
