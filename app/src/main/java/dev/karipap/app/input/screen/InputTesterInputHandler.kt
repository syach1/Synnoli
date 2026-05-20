package dev.karipap.app.input.screen

import dagger.hilt.android.scopes.ActivityScoped
import dev.karipap.app.input.InputTesterController
import dev.karipap.app.input.ScreenInputHandler
import dev.karipap.app.navigation.NavigationController
import javax.inject.Inject

@ActivityScoped
class InputTesterInputHandler @Inject constructor(
    private val nav: NavigationController,
    private val controller: InputTesterController,
) : ScreenInputHandler {
    override fun onBack() = nav.pop()
}
