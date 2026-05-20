package dev.karipap.app.boot

import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.components.ActivityComponent
import dagger.hilt.android.scopes.ActivityScoped
import dev.karipap.app.di.IoScope
import dev.karipap.app.settings.SettingsRepository
import dev.karipap.app.setup.SetupCoordinator
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject

@Module
@InstallIn(ActivityComponent::class)
object BootProviders {

    @Provides
    @ActivityScoped
    fun provideBootSequencer(
        permissionStatus: PermissionStatus,
        settings: SettingsRepository,
        setupCoordinator: SetupCoordinator,
        initializer: Lazy<BootInitializer>,
        startStorageDependentHolder: StartStorageDependentHolder,
        @IoScope ioScope: CoroutineScope,
    ): BootSequencer = BootSequencer(
        permissionStatus = permissionStatus,
        isSetupResolved = { settings.setupCompleted },
        detectVolumes = { setupCoordinator.detectStorageVolumes() + ("Custom" to "") },
        onSetupResolved = { root ->
            if (root != null) settings.sdCardRoot = root
            settings.setupCompleted = true
        },
        startStorageDependent = { startStorageDependentHolder.invoke() },
        initRunner = BootSequencer.InitRunner { onPhase -> initializer.get().run(onPhase) },
        scope = ioScope,
    )
}

/**
 * Lets MainActivity register its `startStorageDependent()` (controller bridge, blacklist,
 * input log) so BootSequencer can invoke it on the NeedsPermission -> Initializing edge,
 * without BootSequencer depending on the Activity.
 */
@ActivityScoped
class StartStorageDependentHolder @Inject constructor() {
    private var action: () -> Unit = {}
    fun register(action: () -> Unit) { this.action = action }
    fun invoke() = action()
}
