package dev.karipap.app.input.screen.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import dev.karipap.app.input.ScreenInputHandler

@Composable
fun ScrollListInput(
    itemCount: Int,
    selectedIndex: Int,
    onMove: (Int) -> Unit,
    onConfirm: () -> Unit = {},
    onBack: () -> Unit = {},
    onWest: (() -> Unit)? = null,
    onNorth: (() -> Unit)? = null,
    onStart: (() -> Unit)? = null,
    onSelect: (() -> Unit)? = null,
    onL1: (() -> Unit)? = null,
    onR1: (() -> Unit)? = null,
    onL2: (() -> Unit)? = null,
    onR2: (() -> Unit)? = null,
) {
    val handler = remember(
        itemCount, selectedIndex,
        onMove, onConfirm, onBack,
        onWest, onNorth, onStart, onSelect,
        onL1, onR1, onL2, onR2,
    ) {
        object : ScreenInputHandler {
            override fun onUp() {
                if (selectedIndex > 0) onMove(selectedIndex - 1)
            }
            override fun onDown() {
                if (selectedIndex < itemCount - 1) onMove(selectedIndex + 1)
            }
            override fun onConfirm() { onConfirm() }
            override fun onBack() { onBack() }
            override fun onWest() { onWest?.invoke() }
            override fun onNorth() { onNorth?.invoke() }
            override fun onStart() { onStart?.invoke() }
            override fun onSelect() { onSelect?.invoke() }
            override fun onL1() { onL1?.invoke() }
            override fun onR1() { onR1?.invoke() }
            override fun onL2() { onL2?.invoke() }
            override fun onR2() { onR2?.invoke() }
        }
    }
    ScreenInput(handler)
}
