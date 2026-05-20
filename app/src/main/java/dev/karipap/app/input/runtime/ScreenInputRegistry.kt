package dev.karipap.app.input.runtime

import dev.karipap.app.input.ScreenInputHandler
import dev.karipap.app.input.screen.EmptyScreenInputHandler
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScreenInputRegistry @Inject constructor() {
    private val stack = mutableListOf<ScreenInputHandler>()

    val top: ScreenInputHandler
        get() = stack.lastOrNull() ?: EmptyScreenInputHandler

    fun push(handler: ScreenInputHandler) {
        stack += handler
    }

    fun pop(handler: ScreenInputHandler) {
        val idx = stack.lastIndexOf(handler)
        if (idx >= 0) stack.removeAt(idx)
    }
}
