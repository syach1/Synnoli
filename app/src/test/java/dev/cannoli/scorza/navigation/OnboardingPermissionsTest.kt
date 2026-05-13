package dev.cannoli.scorza.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OnboardingPermissionsTest {

    private val bothPerms = listOf(OnboardingPermission.STORAGE, OnboardingPermission.BLUETOOTH)
    private val allGranted = bothPerms.toSet()
    private val sampleVolumes = listOf(
        "Internal Storage" to "/storage/emulated/0/",
        "SD card" to "/storage/9C33-6BBD/",
        "Custom" to "",
    )

    private fun screen(
        permissions: List<OnboardingPermission> = bothPerms,
        granted: Set<OnboardingPermission> = emptySet(),
        volumes: List<Pair<String, String>> = emptyList(),
        volumeIndex: Int = 0,
        customPath: String? = null,
        selectedIndex: Int = 0,
    ) = LauncherScreen.OnboardingPermissions(
        permissions = permissions,
        granted = granted,
        volumes = volumes,
        volumeIndex = volumeIndex,
        customPath = customPath,
        selectedIndex = selectedIndex,
    )

    @Test fun movedDownAdvancesFocusThenClampsAtLastIndex() {
        assertEquals(1, screen(selectedIndex = 0).moved(1).selectedIndex)
        assertEquals(1, screen(selectedIndex = 1).moved(1).selectedIndex)
    }

    @Test fun movedUpClampsAtZero() {
        assertEquals(0, screen(selectedIndex = 1).moved(-1).selectedIndex)
        assertEquals(0, screen(selectedIndex = 0).moved(-1).selectedIndex)
    }

    @Test fun movedNeverLeavesRangeWithSingleCard() {
        val single = screen(permissions = listOf(OnboardingPermission.STORAGE))
        assertEquals(0, single.moved(1).selectedIndex)
        assertEquals(0, single.moved(-1).selectedIndex)
    }

    @Test fun focusedPermissionFollowsSelectedIndex() {
        assertEquals(OnboardingPermission.STORAGE, screen(selectedIndex = 0).focusedPermission)
        assertEquals(OnboardingPermission.BLUETOOTH, screen(selectedIndex = 1).focusedPermission)
    }

    @Test fun isFocusedGrantedReflectsGrantedSet() {
        assertFalse(screen(selectedIndex = 0, granted = setOf(OnboardingPermission.BLUETOOTH)).isFocusedGranted)
        assertTrue(screen(selectedIndex = 0, granted = setOf(OnboardingPermission.STORAGE)).isFocusedGranted)
    }

    @Test fun allGrantedRequiresEveryListedPermission() {
        assertFalse(screen(granted = setOf(OnboardingPermission.STORAGE)).allGranted)
        assertTrue(screen(granted = setOf(OnboardingPermission.STORAGE, OnboardingPermission.BLUETOOTH)).allGranted)
        assertTrue(screen(permissions = listOf(OnboardingPermission.STORAGE), granted = setOf(OnboardingPermission.STORAGE)).allGranted)
    }

    @Test fun storageRowIsReachableOnlyWhenAllGranted() {
        val locked = screen(volumes = sampleVolumes, selectedIndex = 1)
        assertEquals(2, locked.focusableCount)
        assertEquals(1, locked.moved(1).selectedIndex)

        val unlocked = screen(granted = allGranted, volumes = sampleVolumes, selectedIndex = 1)
        assertEquals(3, unlocked.focusableCount)
        assertEquals(2, unlocked.moved(1).selectedIndex)
        assertTrue(unlocked.moved(1).isStorageRowFocused)
        assertEquals(null, unlocked.moved(1).focusedPermission)
    }

    @Test fun cycledVolumeWrapsAndResetsCustomPath() {
        val s = screen(granted = allGranted, volumes = sampleVolumes, volumeIndex = 0, customPath = "/storage/x/Cannoli/")
        assertEquals(1, s.cycledVolume(1).volumeIndex)
        assertEquals(null, s.cycledVolume(1).customPath)
        assertEquals(2, s.cycledVolume(-1).volumeIndex)
        val single = screen(granted = allGranted, volumes = listOf("Internal Storage" to "/x/"))
        assertEquals(0, single.cycledVolume(1).volumeIndex)
    }

    @Test fun continueEnabledRequiresGrantsAndValidPath() {
        assertFalse(screen(granted = allGranted, volumes = emptyList()).continueEnabled)
        assertTrue(screen(granted = allGranted, volumes = sampleVolumes, volumeIndex = 0).continueEnabled)
        assertFalse(screen(granted = allGranted, volumes = sampleVolumes, volumeIndex = 2).continueEnabled)
        assertTrue(screen(granted = allGranted, volumes = sampleVolumes, volumeIndex = 2, customPath = "/x/").continueEnabled)
        assertFalse(screen(granted = setOf(OnboardingPermission.STORAGE), volumes = sampleVolumes).continueEnabled)
    }

    @Test fun targetPathPicksCustomOrAppendsCannoliFolder() {
        assertEquals("/storage/emulated/0/Cannoli/",
            screen(granted = allGranted, volumes = sampleVolumes, volumeIndex = 0).targetPath)
        assertEquals("/storage/9C33-6BBD/Cannoli/",
            screen(granted = allGranted, volumes = sampleVolumes, volumeIndex = 1).targetPath)
        assertEquals("/storage/picked/",
            screen(granted = allGranted, volumes = sampleVolumes, volumeIndex = 2, customPath = "/storage/picked/").targetPath)
        assertEquals(null, screen(granted = allGranted, volumes = sampleVolumes, volumeIndex = 2).targetPath)
    }
}
