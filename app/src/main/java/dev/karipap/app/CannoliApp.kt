package dev.karipap.app

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import dev.karipap.app.di.AppFonts
import javax.inject.Inject

@HiltAndroidApp
class CannoliApp : Application() {
    @Inject lateinit var appFonts: AppFonts
}
