package dev.karipap.app.boot

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BootSequencerNextStateTest {

    private val volumes = listOf("Internal Storage" to "/storage/emulated/0/")

    private fun next(
        current: BootState,
        storage: Boolean = true,
        setupKnown: Boolean = true,
    ) = BootSequencer.nextState(
        current = current,
        hasStorage = storage,
        setupResolved = setupKnown,
        volumes = volumes,
    )

    @Test fun resolving_missing_storage_goes_to_needs_permission() {
        assertEquals(
            BootState.NeedsPermission(storageGranted = false),
            next(BootState.Resolving, storage = false),
        )
    }

    @Test fun resolving_granted_but_setup_unknown_goes_to_needs_setup() {
        assertEquals(BootState.NeedsSetup(volumes), next(BootState.Resolving, setupKnown = false))
    }

    @Test fun resolving_granted_and_setup_known_goes_to_initializing() {
        val s = next(BootState.Resolving)
        assertTrue(s is BootState.Initializing && s.phase == BootPhase.IMPORT)
    }

    @Test fun needs_permission_re_evaluates_after_grant() {
        assertTrue(next(BootState.NeedsPermission(false)) is BootState.Initializing)
    }

    @Test fun needs_setup_with_setup_now_known_goes_to_initializing() {
        assertTrue(next(BootState.NeedsSetup(volumes)) is BootState.Initializing)
    }

    @Test fun initializing_is_left_alone() {
        val init = BootState.Initializing(BootPhase.IMPORT, 0.2f, "x")
        assertEquals(init, next(init))
    }

    @Test fun ready_is_left_alone() {
        assertEquals(BootState.Ready, next(BootState.Ready))
    }

    @Test fun error_is_left_alone_until_retry() {
        val err = BootState.Error("boom")
        assertEquals(err, next(err))
    }

    @Test fun losing_storage_permission_from_ready_returns_to_needs_permission() {
        assertEquals(
            BootState.NeedsPermission(storageGranted = false),
            next(BootState.Ready, storage = false),
        )
    }
}
