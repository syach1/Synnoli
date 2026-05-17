package dev.cannoli.scorza.input

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DeviceMatchRuleTest {

    private val device = MatchInput(
        name = "Stadia Controller",
        vendorId = 6353,
        productId = 37888,
        androidBuildModel = "RP4PRO",
        sourceMask = 0x00000401,
    )

    @Test
    fun vendor_and_product_match_scores_100() {
        val rule = DeviceMatchRule(vendorId = 6353, productId = 37888)
        assertEquals(100, rule.score(device))
    }

    @Test
    fun name_only_match_scores_50() {
        val rule = DeviceMatchRule(name = "Stadia Controller")
        assertEquals(50, rule.score(device))
    }

    @Test
    fun android_build_model_match_scores_100() {
        val rule = DeviceMatchRule(androidBuildModel = "RP4PRO")
        assertEquals(100, rule.score(device))
    }

    @Test
    fun source_mask_match_adds_10() {
        val rule = DeviceMatchRule(name = "Stadia Controller", sourceMask = 0x00000401)
        assertEquals(60, rule.score(device))
    }

    @Test
    fun no_fields_set_yields_zero() {
        val rule = DeviceMatchRule()
        assertEquals(0, rule.score(device))
    }

    @Test
    fun vendor_match_without_product_does_not_score_100() {
        val rule = DeviceMatchRule(vendorId = 6353, productId = 0)
        // productId 0 is treated as unset; expect 0 because we require both vid+pid.
        assertEquals(0, rule.score(device))
    }

    @Test
    fun mismatched_name_scores_zero() {
        val rule = DeviceMatchRule(name = "Wrong Controller")
        assertEquals(0, rule.score(device))
    }

    @Test
    fun mismatched_source_mask_does_not_add_bonus() {
        val rule = DeviceMatchRule(name = "Stadia Controller", sourceMask = 0xDEAD)
        // Mask must AND-match a non-zero subset; mismatched mask contributes 0 bonus.
        assertEquals(50, rule.score(device))
    }

    @Test
    fun named_rule_rejects_input_with_different_name_even_when_vid_pid_match() {
        // Retroid kernel fakes the same VID/PID for the internal pad and BT pads, so a saved
        // mapping for the internal pad would otherwise score high on VID/PID + Build.MODEL alone
        // and adopt e.g. a Switch Pro Controller. The name disagreement must be a hard reject.
        val rule = DeviceMatchRule(
            name = "Retroid Pocket Controller",
            vendorId = 8226,
            productId = 12289,
            androidBuildModel = "RP4PRO",
            descriptor = "dc75afea56e3c3a269b97967aa26b8c93c0bd3fb",
        )
        val proPad = MatchInput(
            name = "Nintendo Switch Pro Controller",
            vendorId = 8226,
            productId = 12289,
            androidBuildModel = "RP4PRO",
            sourceMask = 0,
            descriptor = "c575e892a6bb353df4b1327e81beedf84b540eb4",
        )
        assertEquals(0, rule.score(proPad))
    }

    @Test
    fun named_rule_still_matches_input_with_empty_name() {
        // If the kernel leaves the device name empty, we fall back to other signals rather than
        // rejecting; the gate only triggers when both sides name a device and the names differ.
        val rule = DeviceMatchRule(name = "Stadia Controller", vendorId = 6353, productId = 37888)
        val noName = device.copy(name = "")
        assertEquals(100, rule.score(noName))
    }

    @Test
    fun named_rule_matching_second_same_model_pad_still_scores() {
        // Two physically distinct Pro pads share name + VID/PID and have different descriptors.
        // The saved mapping for pad #1 should still win for pad #2 because nothing about the
        // name signals "different device."
        val rule = DeviceMatchRule(
            name = "Nintendo Switch Pro Controller",
            vendorId = 1406,
            productId = 8201,
            descriptor = "first-pad-descriptor",
        )
        val secondPad = MatchInput(
            name = "Nintendo Switch Pro Controller",
            vendorId = 1406,
            productId = 8201,
            androidBuildModel = "RP4PRO",
            sourceMask = 0,
            descriptor = "second-pad-descriptor",
        )
        // No descriptor bonus (different desc), VID/PID match +100, name skipped because VID/PID
        // matched. Still well above zero.
        assertEquals(100, rule.score(secondPad))
    }

    @Test
    fun rule_score_caps_at_100_plus_10() {
        val rule = DeviceMatchRule(
            name = "Stadia Controller",
            vendorId = 6353,
            productId = 37888,
            sourceMask = 0x00000401,
        )
        assertEquals(110, rule.score(device))
    }
}
