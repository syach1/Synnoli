package dev.karipap.app.input.runtime

import dev.karipap.app.input.AnalogRole
import dev.karipap.app.input.CanonicalButton

data class PortSnapshot(
    val pressed: Set<CanonicalButton>,
    val analog: Map<AnalogRole, Float>,
)
