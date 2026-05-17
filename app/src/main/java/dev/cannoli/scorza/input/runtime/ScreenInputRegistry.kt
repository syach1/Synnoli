package dev.cannoli.scorza.input.runtime

import dev.cannoli.scorza.input.ScreenInputHandler
import dev.cannoli.scorza.input.screen.EmptyScreenInputHandler
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
