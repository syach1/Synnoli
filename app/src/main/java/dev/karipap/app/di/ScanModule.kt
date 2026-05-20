package dev.karipap.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.karipap.app.config.CoreInfoRepository
import dev.karipap.app.config.PlatformConfig
import dev.karipap.app.launcher.LaunchManager
import java.io.File
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ScanModule {

    @Provides @Singleton
    fun provideCoreInfoRepository(@ApplicationContext context: Context): CoreInfoRepository {
        val repo = CoreInfoRepository(
            context.assets,
            context.filesDir,
            File(context.applicationInfo.sourceDir).lastModified()
        )
        repo.load()
        return repo
    }

    @Provides @Singleton
    fun providePlatformConfig(
        paths: CannoliPathsProvider,
        @ApplicationContext context: Context,
        coreInfo: CoreInfoRepository,
    ): PlatformConfig {
        val bundledCoresDir = LaunchManager.extractBundledCores(context)
        return PlatformConfig({ paths.root }, context.assets, coreInfo, bundledCoresDir)
    }
}
