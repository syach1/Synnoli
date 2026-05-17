package dev.cannoli.scorza.input.screen.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.staticCompositionLocalOf
import dev.cannoli.scorza.input.ScreenInputHandler
import dev.cannoli.scorza.input.runtime.ScreenInputRegistry

val LocalScreenInputRegistry = staticCompositionLocalOf<ScreenInputRegistry> {
    error("ScreenInputRegistry not provided. Wrap setContent in CompositionLocalProvider.")
}

@Composable
fun ScreenInput(handler: ScreenInputHandler) {
    val registry = LocalScreenInputRegistry.current
    DisposableEffect(handler) {
        registry.push(handler)
        onDispose { registry.pop(handler) }
    }
}
