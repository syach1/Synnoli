package dev.karipap.app.di

import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dev.karipap.app.config.CannoliPaths
import dev.karipap.app.input.autoconfig.AssetCfgSource
import dev.karipap.app.input.autoconfig.AutoconfigLoader
import dev.karipap.app.input.autoconfig.BundledAutoconfigEntries
import dev.karipap.app.input.autoconfig.RetroArchCfgEntry
import dev.karipap.app.input.repo.MappingRepository
import dev.karipap.app.input.resolver.MappingResolver
import dev.karipap.app.input.runtime.ActiveMappingHolder
import dev.karipap.app.input.runtime.ControllerBridge
import dev.karipap.app.input.runtime.PortRouter
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class BundledRetroArchAutoconfig

@Module
@InstallIn(SingletonComponent::class)
object ControllerBindingsModule {

    @Provides
    @Singleton
    fun provideMappingRepository(paths: CannoliPathsProvider): MappingRepository =
        MappingRepository { CannoliPaths(paths.root).configInputMappings }

    @Provides
    @Singleton
    fun provideBundledAutoconfigEntries(
        @ApplicationContext context: Context,
    ): BundledAutoconfigEntries =
        BundledAutoconfigEntries { AutoconfigLoader(AssetCfgSource(context)).entries() }

    @Provides
    @Singleton
    fun provideMappingResolver(
        repository: MappingRepository,
        bundled: BundledAutoconfigEntries,
        paths: CannoliPathsProvider,
        hints: dev.karipap.app.input.hints.ControllerHintTable,
    ): MappingResolver = MappingResolver(
        repository = repository,
        bundledRetroArchEntries = bundled,
        hints = hints,
        mappingsDir = CannoliPaths(paths.root).configInputMappings,
    )

    @Provides
    @Singleton
    fun provideControllerHintTable(
        @ApplicationContext context: Context,
    ): dev.karipap.app.input.hints.ControllerHintTable =
        dev.karipap.app.input.hints.ControllerHintTable.fromAssets(context)

    @Provides
    @Singleton
    fun providePortRouter(): PortRouter = PortRouter()

    @Provides
    @Singleton
    fun provideActiveMappingHolder(): ActiveMappingHolder = ActiveMappingHolder()

    @Provides
    @Singleton
    fun provideControllerBridge(
        resolver: MappingResolver,
        portRouter: PortRouter,
        activeMappingHolder: ActiveMappingHolder,
        mappingRepository: MappingRepository,
        blacklist: dev.karipap.app.input.ControllerBlacklist,
        bundled: BundledAutoconfigEntries,
        hints: dev.karipap.app.input.hints.ControllerHintTable,
    ): ControllerBridge = ControllerBridge(
        resolver = resolver,
        portRouter = portRouter,
        activeMappingHolder = activeMappingHolder,
        mappingRepository = mappingRepository,
        blacklist = blacklist,
        bundledCfgs = bundled,
        hints = hints,
    )
}
