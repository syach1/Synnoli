package dev.karipap.app.input.runtime

import dev.karipap.app.input.AnalogRole
import dev.karipap.app.input.CanonicalButton

sealed interface CanonicalEvent {
    data class Pressed(val button: CanonicalButton) : CanonicalEvent
    data class Released(val button: CanonicalButton) : CanonicalEvent
    data class AnalogChanged(val role: AnalogRole, val value: Float) : CanonicalEvent
}
