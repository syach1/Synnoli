package dev.karipap.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.karipap.app.config.CoreInfoRepository
import dev.karipap.app.config.PlatformConfig
import dev.karipap.app.launcher.ApkLauncher
import dev.karipap.app.launcher.EmuLauncher
import dev.karipap.app.launcher.InstalledCoreService
import dev.karipap.app.launcher.LaunchManager
import dev.karipap.app.launcher.RetroArchLauncher
import dev.karipap.app.settings.SettingsRepository
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LaunchModule {

    @Provides @Singleton
    fun provideRetroArchLauncher(
        @ApplicationContext context: Context,
        settings: SettingsRepository,
    ): RetroArchLauncher = RetroArchLauncher(context) { settings.retroArchPackage }

    @Provides @Singleton
    fun provideLaunchManager(
        @ApplicationContext context: Context,
        settings: SettingsRepository,
        platformConfig: PlatformConfig,
        retroArchLauncher: RetroArchLauncher,
        emuLauncher: EmuLauncher,
        apkLauncher: ApkLauncher,
        installedCoreService: InstalledCoreService,
        coreInfo: CoreInfoRepository,
    ): LaunchManager = LaunchManager(
        context, settings, platformConfig,
        retroArchLauncher, emuLauncher, apkLauncher, installedCoreService, coreInfo
    )
}
