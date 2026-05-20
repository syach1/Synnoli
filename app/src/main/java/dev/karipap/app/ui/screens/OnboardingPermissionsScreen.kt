package dev.karipap.app.ui.screens

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.karipap.app.R
import dev.karipap.app.navigation.OnboardingPermission
import dev.cannoli.ui.ButtonStyle
import dev.cannoli.ui.DPAD_HORIZONTAL
import dev.cannoli.ui.START_GLYPH
import dev.cannoli.ui.components.BottomBar
import dev.cannoli.ui.components.footerReservation
import dev.cannoli.ui.components.screenPadding
import dev.cannoli.ui.theme.CannoliTypography
import dev.cannoli.ui.theme.GrayText
import dev.cannoli.ui.theme.LocalCannoliColors
import dev.cannoli.ui.theme.LocalCannoliTypography
import dev.cannoli.ui.theme.Spacing

private val GrantedGreen = Color(0xFF4CAF50)
private val UnfocusedBorder = Color(0xFF333333)

@Composable
fun OnboardingPermissionsScreen(
    permissions: List<OnboardingPermission>,
    granted: Set<OnboardingPermission>,
    volumes: List<Pair<String, String>>,
    volumeIndex: Int,
    customPath: String?,
    romDirectory: String?,
    biosDirectory: String?,
    selectedIndex: Int,
    buttonStyle: ButtonStyle = ButtonStyle(),
) {
    val typo = LocalCannoliTypography.current
    val colors = LocalCannoliColors.current
    val accent = colors.accent
    val allGranted = granted.containsAll(permissions)
    val storageRowIndex = permissions.size
    val romRowIndex = storageRowIndex + 1
    val biosRowIndex = storageRowIndex + 2
    val isStorageRowFocused = allGranted && selectedIndex == storageRowIndex
    val isRomRowFocused = allGranted && selectedIndex == romRowIndex
    val isBiosRowFocused = allGranted && selectedIndex == biosRowIndex
    val selectedVolume = volumes.getOrNull(volumeIndex)
    val isCustom = selectedVolume?.first == "Custom"

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(screenPadding)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = footerReservation())
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.onboarding_header),
                style = typo.labelSmall,
                color = colors.text.copy(alpha = 0.6f),
            )
            Spacer(modifier = Modifier.height(Spacing.Sm))
            Text(
                text = stringResource(R.string.onboarding_permissions_title),
                style = typo.titleLarge,
                color = Color.White,
            )
            Spacer(modifier = Modifier.height(Spacing.Lg))

            permissions.forEachIndexed { index, perm ->
                if (index > 0) Spacer(modifier = Modifier.height(Spacing.Md))
                PermissionCard(
                    label = stringResource(R.string.onboarding_storage_label),
                    rationale = stringResource(R.string.onboarding_storage_rationale),
                    isGranted = perm in granted,
                    isFocused = index == selectedIndex,
                    accent = accent,
                    typo = typo,
                )
            }

            Spacer(modifier = Modifier.height(Spacing.Md))

            PathCard(
                label = stringResource(R.string.setup_storage),
                value = when {
                    isCustom && customPath != null -> customPath
                    isCustom -> stringResource(R.string.setup_folder_unset)
                    else -> selectedVolume?.first ?: stringResource(R.string.setup_folder_unset)
                },
                isLocked = !allGranted,
                isFocused = isStorageRowFocused,
                customPickPrompt = isStorageRowFocused && isCustom && customPath == null,
                accent = accent,
                typo = typo,
            )

            Spacer(modifier = Modifier.height(Spacing.Md))

            PathCard(
                label = stringResource(R.string.setting_rom_directory),
                value = romDirectory ?: stringResource(R.string.setup_folder_unset),
                isLocked = !allGranted,
                isFocused = isRomRowFocused,
                customPickPrompt = isRomRowFocused,
                accent = accent,
                typo = typo,
            )

            Spacer(modifier = Modifier.height(Spacing.Md))

            PathCard(
                label = stringResource(R.string.setting_bios_directory),
                value = biosDirectory ?: stringResource(R.string.setup_folder_unset),
                isLocked = !allGranted,
                isFocused = isBiosRowFocused,
                customPickPrompt = isBiosRowFocused,
                accent = accent,
                typo = typo,
            )
        }

        val leftItems = mutableListOf(buttonStyle.back to stringResource(R.string.label_quit))
        if (isStorageRowFocused && volumes.size > 1) {
            leftItems.add(DPAD_HORIZONTAL to stringResource(R.string.label_change))
        }
        val rightItems = mutableListOf<Pair<String, String>>()
        val focusedPerm = permissions.getOrNull(selectedIndex)
        if (focusedPerm != null && focusedPerm !in granted) {
            rightItems.add(buttonStyle.confirm to stringResource(R.string.label_grant))
        }
        if (isStorageRowFocused && isCustom && customPath == null) {
            rightItems.add(buttonStyle.confirm to stringResource(R.string.onboarding_select_folder))
        }
        if (isRomRowFocused || isBiosRowFocused) {
            rightItems.add(buttonStyle.confirm to stringResource(R.string.onboarding_select_folder))
        }
        val continueEnabled = allGranted && volumes.isNotEmpty() && (!isCustom || customPath != null)
        if (continueEnabled) {
            rightItems.add(START_GLYPH to stringResource(R.string.label_continue))
        }
        BottomBar(
            modifier = Modifier.align(Alignment.BottomCenter),
            leftItems = leftItems,
            rightItems = rightItems,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PermissionCard(
    label: String,
    rationale: String,
    isGranted: Boolean,
    isFocused: Boolean,
    accent: Color,
    typo: CannoliTypography,
) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(isFocused) { if (isFocused) requester.bringIntoView() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bringIntoViewRequester(requester)
            .border(2.dp, if (isFocused) accent else UnfocusedBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = Spacing.Md, vertical = Spacing.Sm),
    ) {
        StatusRow(typo = typo, label = label, isGranted = isGranted)
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = rationale, style = typo.bodyMedium, color = GrayText)
        if (isFocused && !isGranted) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.onboarding_press_a_to_grant), style = typo.bodyMedium, color = accent)
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PathCard(
    label: String,
    value: String,
    isLocked: Boolean,
    isFocused: Boolean,
    customPickPrompt: Boolean,
    accent: Color,
    typo: CannoliTypography,
) {
    val requester = remember { BringIntoViewRequester() }
    LaunchedEffect(isFocused) { if (isFocused) requester.bringIntoView() }
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (isLocked) 0.35f else 1f)
            .bringIntoViewRequester(requester)
            .border(2.dp, if (isFocused) accent else UnfocusedBorder, RoundedCornerShape(8.dp))
            .padding(horizontal = Spacing.Md, vertical = Spacing.Sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(text = label, style = typo.bodyLarge, color = Color.White)
            Text(
                text = value,
                modifier = Modifier
                    .weight(1f)
                    .padding(start = Spacing.Md),
                style = typo.bodyMedium,
                color = GrayText,
                textAlign = TextAlign.End,
            )
        }
        if (isLocked) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.onboarding_storage_locked_hint), style = typo.bodyMedium, color = GrayText)
        } else if (customPickPrompt) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = stringResource(R.string.onboarding_press_a_to_select_folder), style = typo.bodyMedium, color = accent)
        }
    }
}

@Composable
private fun StatusRow(typo: CannoliTypography, label: String, isGranted: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(text = label, style = typo.bodyLarge, color = Color.White)
        Text(
            text = if (isGranted) stringResource(R.string.onboarding_status_granted)
            else stringResource(R.string.onboarding_status_not_granted),
            style = typo.bodyMedium,
            color = if (isGranted) GrantedGreen else GrayText,
        )
    }
}
